package com.eventflow.eventflow.transaction;

public record LockResource(
        String table,
        String objectId
) {
    public LockResource {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be empty."
            );
        }

        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException(
                    "Object ID cannot be empty."
            );
        }

        table = table.toLowerCase();
    }
}
