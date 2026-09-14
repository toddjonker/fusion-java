// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion;

import static dev.ionfusion.commons.util.Empties.EMPTY_OBJECT_ARRAY;

import dev.ionfusion.commons.resources.ResourcePosition;

abstract class SyntaxContainer
    extends SyntaxValue
{

    SyntaxContainer(ResourcePosition pos, Object[] properties, SyntaxWraps wraps)
    {
        super(wraps, pos, properties);
    }

    SyntaxContainer(ResourcePosition pos)
    {
        super(null, pos, EMPTY_OBJECT_ARRAY);
    }
}
