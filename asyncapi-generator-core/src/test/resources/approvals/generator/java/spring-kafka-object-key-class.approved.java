package com.example.account.model;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Identifies an account within an institution.
 */
public class MyAccountKey implements Serializable {

    @NotNull
    private String institutionId;

    @NotNull
    private String accountId;

    public MyAccountKey() {
        // Default constructor
    }

    // All-args constructor
    public MyAccountKey(
        String institutionId,
        String accountId
    ) {
        this.institutionId = institutionId;
        this.accountId = accountId;
    }

    /**
     * Get institutionId.
     * Institution that owns the account.
     * @return String
     */
    public String getInstitutionId() {
        return institutionId;
    }

    /**
     * Set institutionId.
     * @param institutionId Institution that owns the account.
     */
    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    /**
     * Get accountId.
     * Account identifier within the institution.
     * @return String
     */
    public String getAccountId() {
        return accountId;
    }

    /**
     * Set accountId.
     * @param accountId Account identifier within the institution.
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyAccountKey that = (MyAccountKey) o;
        return
            Objects.equals(institutionId, that.institutionId) &&

            Objects.equals(accountId, that.accountId)
;
    }

    @Override
    public int hashCode() {
        return Objects.hash(

            institutionId,
            accountId
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MyAccountKey {\n");
        sb.append("    institutionId: ").append(institutionId).append("\n");
        sb.append("    accountId: ").append(accountId).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
