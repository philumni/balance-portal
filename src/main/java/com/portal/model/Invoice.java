package com.portal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Invoice POJO.
 * Jackson serializes this directly to JSON for the /api/invoices response.
 */
public class Invoice {

    public enum Status {
        PAID, UNPAID, OVERDUE, PENDING;

        public String getLabel() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }

        public String getCssClass() {
            switch (this) {
                case PAID:    return "status-paid";
                case UNPAID:  return "status-unpaid";
                case OVERDUE: return "status-overdue";
                case PENDING: return "status-pending";
                default:      return "";
            }
        }
    }

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private String    invoiceNumber;
    private String    description;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private double    amount;
    private Status    status;

    public Invoice(String invoiceNumber, String description,
                   LocalDate invoiceDate, LocalDate dueDate,
                   double amount, Status status) {
        this.invoiceNumber = invoiceNumber;
        this.description   = description;
        this.invoiceDate   = invoiceDate;
        this.dueDate       = dueDate;
        this.amount        = amount;
        this.status        = status;
    }

    // --- Getters (Jackson uses these to build JSON) ---

    public String    getInvoiceNumber() { return invoiceNumber; }
    public String    getDescription()   { return description; }
    public double    getAmount()        { return amount; }
    public Status    getStatus()        { return status; }

    // Send formatted strings so JS doesn't need to reformat
    @JsonProperty("invoiceDate")
    public String getFormattedInvoiceDate() { return invoiceDate.format(DISPLAY_FMT); }

    @JsonProperty("dueDate")
    public String getFormattedDueDate()     { return dueDate.format(DISPLAY_FMT); }

    @JsonProperty("formattedAmount")
    public String getFormattedAmount()      { return String.format("$%,.2f", amount); }

    @JsonProperty("statusLabel")
    public String getStatusLabel()          { return status.getLabel(); }

    @JsonProperty("statusCss")
    public String getStatusCss()            { return status.getCssClass(); }

    // Raw LocalDate — excluded from JSON (use the formatted strings above)
    @JsonIgnore
    public LocalDate getInvoiceDate()       { return invoiceDate; }

    @JsonIgnore
    public LocalDate getDueDate()           { return dueDate; }

    public boolean isOutstanding() {
        return status == Status.UNPAID
            || status == Status.OVERDUE
            || status == Status.PENDING;
    }
}
