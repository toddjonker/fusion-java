// Copyright (c) 2012 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

/**
 * Extended prepare-time {@link Namespace} that knows it's a module.
 * This exists to create special bindings that can refer module variables that
 * are not exported (but that are accessible through macro-generated code).
 */
class ModuleNamespace
    extends Namespace
{
    static final class ModuleBinding
        extends TopBinding
    {
        private final ModuleIdentity myModuleId;

        private ModuleBinding(SyntaxSymbol identifier, int address,
                              ModuleIdentity moduleId)
        {
            super(identifier, address);
            myModuleId = moduleId;
        }

        @Override
        public Object lookup(Environment env)
        {
            Namespace localNamespace = env.namespace();
            if (localNamespace.getModuleId() != myModuleId)
            {
                // The local context is a different module, so we must ignore
                // it and go directly to our own namespace.

                ModuleInstance module =
                    localNamespace.getRegistry().lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;

                NamespaceStore ns = module.getNamespace();
                return ns.lookup(myAddress);
            }

            return localNamespace.lookup(this);
        }

        // TODO FUSION-83 registry shouldn't be needed
        Object lookup(ModuleInstance module, ModuleRegistry registry)
        {
            if (module.getIdentity() != myModuleId)
            {
                module = registry.lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;
            }

            NamespaceStore ns = module.getNamespace();
            return ns.lookup(myAddress);
        }

        @Override
        public CompiledForm compileReference(Evaluator eval, Environment env)
            throws FusionException
        {
            Namespace localNamespace = env.namespace();
            if (localNamespace.getModuleId() != myModuleId)
            {
                // We have a reference to a binding from another module!
                // Compiled form must include link to the module since it
                // won't be the top of the runtime environment chain.

                ModuleInstance module =
                    localNamespace.getRegistry().lookup(myModuleId);
                assert module != null : "Module not found: " + myModuleId;

                NamespaceStore ns = module.getNamespace();
                return new CompiledImportedVariableReference(ns, myAddress);
            }

            return super.compileReference(eval, env);
        }

        @Override
        public String toString()
        {
            return "{{ModuleBinding " + getName() + "}}";
        }
    }


    private final ModuleIdentity myModuleId;

    ModuleNamespace(ModuleRegistry registry, ModuleIdentity moduleId)
    {
        super(registry);
        myModuleId = moduleId;
    }

    ModuleNamespace(ModuleRegistry registry, ModuleInstance language,
                    ModuleIdentity moduleId)
    {
        super(registry, language);
        myModuleId = moduleId;
    }


    @Override
    ModuleIdentity getModuleId()
    {
        return myModuleId;
    }


    @Override
    TopBinding newBinding(SyntaxSymbol identifier, int address)
    {
        assert identifier.uncachedResolve() instanceof FreeBinding;
        return new ModuleBinding(identifier, address, myModuleId);
    }


    //========================================================================


    /**
     * A reference to a top-level variable in a namespace that is not the one
     * in our lexical context.
     */
    private static final class CompiledImportedVariableReference
        implements CompiledForm
    {
        private final NamespaceStore myNamespace;
        private final int            myAddress;

        CompiledImportedVariableReference(NamespaceStore namespace, int address)
        {
            myNamespace = namespace;
            myAddress   = address;
        }

        @Override
        public Object doEval(Evaluator eval, Store store)
            throws FusionException
        {
            Object result = myNamespace.lookup(myAddress);
            assert result != null : "No value for " + myAddress;
            return result;
        }
    }

}
