// Copyright Ion Fusion contributors. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package dev.ionfusion.fusion._private;

import java.util.Objects;

public class StringSymbol {
    private final String value;

    StringSymbol(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof StringSymbol &&
               Objects.equals(value, ((StringSymbol) o).value));
    }

    @Override
    public int hashCode() { return Objects.hashCode(value); }
}
