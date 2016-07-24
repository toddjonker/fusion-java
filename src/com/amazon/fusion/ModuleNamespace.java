// Copyright (c) 2012-2016 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.FusionSymbol.BaseSymbol;
import com.amazon.fusion.LanguageWrap.LanguageBinding;
import com.amazon.fusion.TopLevelNamespace.TopLevelBinding;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


/**
 * Extended prepare-time {@link Namespace} that knows it's a module.
 * This exists to create special bindings that can refer module variables that
 * are not exported (but that are accessible through macro-generated code).
 */
final class ModuleNamespace
    extends Namespace
{
    static abstract class ProvidedBinding
        extends Binding
    {
        private final BaseSymbol myName;

        ProvidedBinding(SyntaxSymbol exportedId)
        {
            myName = exportedId.getName();
        }

        @Override
        final BaseSymbol getName() { return myName; }

        @Override
        final ProvidedBinding provideAs(SyntaxSymbol exportedId)
        {
            return new ImportedProvidedBinding(exportedId, this);
        }

        @Override
        abstract ModuleDefinedBinding target();
        abstract ModuleIdentity getTargetModule();

        @Override
        final Object lookup(Namespace ns)
        {
            return target().lookup(ns);
        }

        @Override
        final CompiledForm compileReference(Evaluator eval, Environment env)
            throws FusionException
        {
            return target().compileReference(eval, env);
        }

        @Override
        final CompiledForm compileTopReference(Evaluator eval, Environment env,
                                               SyntaxSymbol id)
            throws FusionException
        {
            return target().compileLocalTopReference(eval, env);
        }

        @Override
        final public CompiledForm compileSet(Evaluator eval, Environment env,
                                             CompiledForm valueForm)
            throws FusionException
        {
            // This isn't currently reachable, but it's an easy safeguard.
            String message = "Mutation of imported binding is not allowed";
            throw new ContractException(message);
        }
    }

    /**
     * A provided binding that was defined in the providing module.
     */
    static final class DefinedProvidedBinding
        extends ProvidedBinding
    {
        private final ModuleDefinedBinding myDefinition;

        DefinedProvidedBinding(SyntaxSymbol exportedId,
                               ModuleDefinedBinding binding)
        {
            super(exportedId);

            assert binding.target() == binding;
            myDefinition = binding;
        }

        DefinedProvidedBinding(ModuleDefinedBinding binding)
        {
            this(binding.getIdentifier(), binding);
        }

        @Override
        ModuleDefinedBinding target()
        {
            return myDefinition;
        }

        @Override
        ModuleIdentity getTargetModule()
        {
            return myDefinition.myModuleId;
        }

        @Override
        public String toString()
        {
            return "{{{DefinedProvidedBinding " + getName()
                + " -> "  + myDefinition + "}}}";
        }
    }

    /**
     * A provided binding that was imported into the providing module.
     */
    static final class ImportedProvidedBinding
        extends ProvidedBinding
    {
        private final ProvidedBinding myImport;

        ImportedProvidedBinding(SyntaxSymbol exportedId, ProvidedBinding imported)
        {
            super(exportedId);
            myImport = imported;
        }

        @Override
        ModuleDefinedBinding target()
        {
            return myImport.target();
        }

        @Override
        ModuleIdentity getTargetModule()
        {
            return myImport.getTargetModule();
        }

        @Override
        public String toString()
        {
            return "{{{ImportedProvidedBinding " + getName()
                + " -> "  + myImport + "}}}";
        }
    }


    /**
     * Denotes a module-level binding imported into a module via
     * {@code require}.
     *
     * @see LanguageBinding
     */
    static final class ModuleRequiredBinding
        extends RequiredBinding
    {
        private ModuleRequiredBinding(SyntaxSymbol identifier,
                                      ProvidedBinding target)
        {
            super(identifier, target);
        }

        @Override
        ProvidedBinding provideAs(SyntaxSymbol exportedId)
        {
            return new ImportedProvidedBinding(exportedId, myTarget);
        }

        @Override
        public CompiledForm compileTopReference(Evaluator eval,
                                                Environment env,
                                                SyntaxSymbol id)
            throws FusionException
        {
            String message =
                "#%top not implemented for top-require binding: " + this;
            throw new IllegalStateException(message);
        }

        @Override
        public boolean equals(Object other)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString()
        {
            return "{{{ModuleRequiredBinding "
                 + target().myModuleId.absolutePath()
                 + ' ' + getName() + "}}}";
        }
    }


    /**
     * Exposes the bindings required at module-level.
     * The "next wrap" is always a {@link LanguageWrap}.
     */
    private final class ModuleRequiresWrap
        extends SyntaxWrap
    {
        @Override
        Binding resolve(BaseSymbol           name,
                        Iterator<SyntaxWrap> moreWraps,
                        Set<MarkWrap>        returnMarks)
        {
            assert moreWraps.hasNext() : "Missing language wrap";

            Binding langBinding =
                moreWraps.next().resolve(name, moreWraps, returnMarks);

            // Look for an imported binding.
            // TODO FUSION-117 Resolve the whole identifier, including marks.
            ModuleRequiredBinding requiredBinding =
                myRequiredBindings.get(name, Collections.<MarkWrap>emptySet());

            return (requiredBinding != null ? requiredBinding : langBinding);
        }

        @Override
        Iterator<SyntaxWrap> iterator()
        {
            return null;
        }

        @Override
        public String toString()
        {
            ModuleIdentity id = getModuleId();
            return "{{{ModuleRequiresWrap for " + id.absolutePath() + "}}}";
        }
    }



    /**
     * Denotes a binding defined (not imported) at module-level.
     * Instances are one-to-one with each {@code define} at module-level.
     * <p>
     * Unlike top-level bindings, module-level bindings are immutable.
     * <p>
     * When imported into another namespace, a {@code ModuleDefinedBinding} is
     * wrapped by either a {@link LanguageBinding} or a
     * {@link Namespace.RequiredBinding}.
     */
    static final class ModuleDefinedBinding
        extends NsBinding
    {
        final ModuleIdentity myModuleId;

        private ModuleDefinedBinding(SyntaxSymbol identifier, int address,
                                     ModuleIdentity moduleId)
        {
            super(identifier, address);
            myModuleId = moduleId;
        }

        @Override
        ProvidedBinding provideAs(SyntaxSymbol exportedId)
        {
            return new DefinedProvidedBinding(exportedId, this);
        }

        @Override
        public Object lookup(Namespace localNamespace)
        {
            if (localNamespace.getModuleId() != myModuleId)
            {
                // The local context is a different module, so we must ignore
                // it and go directly to our own namespace.

                ModuleInstance module =
                    localNamespace.getRegistry().lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;

                ModuleStore ns = module.getNamespace();
                return ns.lookup(myAddress);
            }

            // We can't use our address directly, since we may be compiling
            // and the binding may not have a location allocated yet.
            return localNamespace.lookupDefinition(this);
        }

        Object lookup(ModuleInstance module)
        {
            ModuleStore ns = module.getNamespace();

            if (module.getIdentity() != myModuleId)
            {
                module = ns.getRegistry().lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;
                ns = module.getNamespace();
            }

            return ns.lookup(myAddress);
        }

        @Override
        public String mutationSyntaxErrorMessage()
        {
             return "mutation of module-level bindings is not yet supported";
        }

        @Override
        CompiledForm compileDefine(Evaluator eval,
                                   Environment env,
                                   SyntaxSymbol id,
                                   CompiledForm valueForm)
            throws FusionException
        {
            return env.namespace().compileDefine(eval, this, id, valueForm);
        }

        @Override
        public CompiledForm compileTopReference(Evaluator eval,
                                                Environment env,
                                                SyntaxSymbol id)
            throws FusionException
        {
            // We should never get here.
            String message =
                "#%top not implemented for module binding: " + this;
            throw new SyntaxException("#%top", message, id);
        }

        @Override
        public CompiledForm compileReference(Evaluator eval, Environment env)
            throws FusionException
        {
            Namespace localNamespace = env.namespace();
            if (localNamespace.getModuleId() != myModuleId)
            {
                // We have a reference to a binding from another module!
                // Compiled form must include address of the module since it
                // won't be the top of the runtime environment chain.

                int moduleAddress =
                    localNamespace.requiredModuleAddress(myModuleId);

                return new CompiledImportedVariableReference(moduleAddress,
                                                             myAddress);
            }

            return compileLocalTopReference(eval, env);
        }

        @Override
        public String toString()
        {
            return "{{{ModuleDefinedBinding " + myModuleId.absolutePath()
                + ' ' + getIdentifier().debugString() + "}}}";
        }
    }


    /**
     * Exposes the bindings defined at module-level.
     */
    private static final class ModuleDefinesWrap
        extends EnvironmentWrap
    {
        ModuleDefinesWrap(ModuleNamespace ns)
        {
            super(ns);
        }

        @Override
        Binding resolveTop(BaseSymbol name,
                           Iterator<SyntaxWrap> moreWraps,
                           Set<MarkWrap> returnMarks)
        {
            if (moreWraps.hasNext())
            {
                SyntaxWrap nextWrap = moreWraps.next();
                return nextWrap.resolve(name, moreWraps, returnMarks);
            }
            return null;
        }

        @Override
        public String toString()
        {
            ModuleIdentity id =
                ((ModuleNamespace) getEnvironment()).getModuleId();
            return "{{{ModuleDefinesWrap " + id.absolutePath() + "}}}";
        }
    }



    /**
     * Maps each imported identifier to its binding. Doesn't include language
     * bindings.
     */
    private final RequiredBindingMap<ModuleRequiredBinding> myRequiredBindings =
        new RequiredBindingMap<ModuleRequiredBinding>() {
            @Override
            void checkReplacement(ModuleRequiredBinding current,
                                  ModuleRequiredBinding replacement)
                throws AmbiguousBindingFailure
            {
                if (! current.sameTarget(replacement))
                {
                    String name = current.getIdentifier().stringValue();
                    throw new AmbiguousBindingFailure(GlobalState.REQUIRE,
                                                      name);
                }
            }
        };


    /**
     * Constructs a module with a given language.  Bindings provided by the
     * language can be shadowed by {@code require} or {@code define}.
     *
     * @param moduleId identifies this module.
     */
    ModuleNamespace(ModuleRegistry registry,
                    final ModuleInstance language,
                    ModuleIdentity moduleId)
    {
        super(registry, moduleId,
              new Function<Namespace, SyntaxWraps>()
              {
                  @Override
                  public SyntaxWraps apply(Namespace _this) {
                      ModuleNamespace __this = (ModuleNamespace) _this;
                      return SyntaxWraps.make(new ModuleDefinesWrap(__this),
                                              __this.new ModuleRequiresWrap(),
                                              new LanguageWrap(language));
                  }
              });
    }

    /**
     * Constructs a module that uses no other module. Any bindings will need to
     * be created via {@link #bind(String, Object)}.
     *
     * @param moduleId identifies this module.
     */
    ModuleNamespace(ModuleRegistry registry, ModuleIdentity moduleId)
    {
        super(registry, moduleId,
              new Function<Namespace, SyntaxWraps>()
              {
                  @Override
                  public SyntaxWraps apply(Namespace _this) {
                      return SyntaxWraps.make(new EnvironmentWrap(_this));
                  }
              });
    }


    @Override
    NsBinding newDefinedBinding(SyntaxSymbol identifier, int address)
    {
        return new ModuleDefinedBinding(identifier, address, getModuleId());
    }

    @Override
    public void setDoc(int address, BindingDoc doc)
    {
        doc.addProvidingModule(getModuleId());

        super.setDoc(address, doc);
    }


    @Override
    SyntaxSymbol predefine(SyntaxSymbol identifier, SyntaxValue formForErrors)
        throws FusionException
    {
        // Don't cache the binding! The symbol instance may also be in use as
        // a reference to this binding (due to a macro expansion), in which
        // case this result is not correct for that reference.
        Binding oldBinding = identifier.uncachedResolveMaybe();
        if (oldBinding == null ||
            oldBinding instanceof FreeBinding)
        {
            identifier = identifier.copyAndResolveTop();
        }
        else if (oldBinding instanceof LanguageBinding)
        {
            // Visible binding is from our language, we can shadow it.
            // Again, be careful not to cache a binding in the original id.
            identifier = identifier.copyReplacingBinding(oldBinding);
        }
        else // there's an imported binding
        {
            String name = identifier.stringValue();
            throw new AmbiguousBindingFailure(null, name, formForErrors);
        }

        NsBinding b = addDefinedBinding(identifier);
        return identifier.copyReplacingBinding(b);
    }


    @Override
    void require(Evaluator eval, ModuleInstance module)
        throws FusionException
    {
        for (ProvidedBinding provided : module.providedBindings())
        {
            // TODO FUSION-117 Not sure this is the right lexical context.
            // The identifier is free, but references will have context
            // including the language.
            SyntaxSymbol id = SyntaxSymbol.make(eval, null, provided.getName());

            ModuleRequiredBinding b = new ModuleRequiredBinding(id, provided);
            myRequiredBindings.put(id, b);
        }
    }


    @Override
    CompiledForm compileDefine(Evaluator eval,
                               FreeBinding binding,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        throw new IllegalStateException("Unexpected define in module: "
                                        + binding);
    }


    @Override
    CompiledForm compileDefine(Evaluator eval,
                               TopLevelBinding binding,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        throw new IllegalStateException("Unexpected define in module: "
                                        + binding);
    }


    @Override
    CompiledForm compileDefine(Evaluator eval,
                               ModuleDefinedBinding binding,
                               SyntaxSymbol id,
                               CompiledForm valueForm)
        throws FusionException
    {
        String name = binding.getName().stringValue();
        return new CompiledTopDefine(name, binding.myAddress, valueForm);

    }


    @Override
    CompiledForm compileFreeTopReference(SyntaxSymbol identifier)
        throws FusionException
    {
        throw new IllegalStateException("Unexpected #%top in module: "
                                        + identifier);
    }


    //========================================================================


    /**
     * A reference to a top-level variable in a namespace that is not the one
     * in our lexical context.
     */
    static final class CompiledImportedVariableReference
        implements CompiledForm
    {
        final int myModuleAddress;
        final int myBindingAddress;

        CompiledImportedVariableReference(int moduleAddress,
                                          int bindingAddress)
        {
            myModuleAddress  = moduleAddress;
            myBindingAddress = bindingAddress;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            NamespaceStore ns = store.namespace();
            Object result = ns.lookupImport(myModuleAddress, myBindingAddress);
            assert result != null
                : "No value for " + myModuleAddress + "@" + myBindingAddress;
            return result;
        }
    }

}
