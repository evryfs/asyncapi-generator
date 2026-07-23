package com.example.account.model;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Identifies an account within an institution.
 * @param institutionId Institution that owns the account.
 * @param accountId Account identifier within the institution.
 */
public record MyAccountKey(
    @NotNull
    String institutionId,

    @NotNull
    String accountId
) implements Serializable {
}
