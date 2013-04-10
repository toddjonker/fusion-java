// Copyright (c) 2012-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.fusion;

import com.amazon.fusion.BindingDoc.Kind;
import com.amazon.fusion.Namespace.TopBinding;
import java.util.Collection;


final class ModuleBuilderImpl
    implements ModuleBuilder
{
    private final ModuleRegistry  myRegistry;
    private final ModuleNamespace myNamespace;

    ModuleBuilderImpl(ModuleRegistry registry, ModuleIdentity moduleId)
    {
        myRegistry = registry;
        myNamespace = new ModuleNamespace(registry, moduleId);
    }

    @Override
    public void define(String name, Object value)
        throws FusionException
    {
        myNamespace.bind(name, value);

        if (value instanceof FusionValue)
        {
            BindingDoc doc = ((FusionValue) value).document();
            if (doc != null)
            {
                myNamespace.setDoc(name, doc);
            }
        }
    }

    void define(String name, Object value, String documentation)
        throws FusionException
    {
        myNamespace.bind(name, value);

        Kind kind =
            (value instanceof Procedure ? Kind.PROCEDURE : Kind.CONSTANT);
        myNamespace.setDoc(name, kind, documentation);
    }

    ModuleInstance build()
        throws FusionException
    {
        ModuleStore store = new ModuleStore(myRegistry,
                                            myNamespace.extractValues(),
                                            myNamespace.extractBindingDocs());
        Collection<TopBinding> bindings = myNamespace.getBindings();

        // TODO should we register the module?
        return new ModuleInstance(myNamespace.getModuleId(), store, bindings);
    }

    @Override
    public void instantiate()
        throws FusionException
    {
        ModuleInstance module = build();
        myRegistry.register(module);
    }
}
