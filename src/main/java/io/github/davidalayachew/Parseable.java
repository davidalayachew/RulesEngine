package io.github.davidalayachew;


public sealed interface Parseable
         permits
            Identifier,
            Type,
            IdentifierIsAType,
            IdentifierHasQuantityType,
            Quantity,
            QuantityType,
            FrequencyType,
            FrequencyTypeHasQuantityType,
            FrequencyTypeIsType,
            FrequencyTypeRelationship,
            IsIdentifierAType
{}
