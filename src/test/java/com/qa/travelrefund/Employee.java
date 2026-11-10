package com.qa.travelrefund;

/** Employee test data, as pulled by the system from the services/DB. */
public final class Employee {

    public enum Type { COMPANY, EXTERNAL, OUTSOURCED, SHUTTLE }

    public final String id;
    public final Type type;
    public final String residence;
    public final String workplace;
    public final double distanceKm;
    public final int jobPercent;
    public final int monthWorkDays;
    public final int actualWorkDays;

    public Employee(String id, Type type, String residence, String workplace,
                    double distanceKm, int jobPercent, int monthWorkDays, int actualWorkDays) {
        this.id = id;
        this.type = type;
        this.residence = residence;
        this.workplace = workplace;
        this.distanceKm = distanceKm;
        this.jobPercent = jobPercent;
        this.monthWorkDays = monthWorkDays;
        this.actualWorkDays = actualWorkDays;
    }

    /** Same employee as if they had worked every work day of the month. */
    public Employee withFullMonth() {
        return new Employee(id, type, residence, workplace, distanceKm,
                jobPercent, monthWorkDays, monthWorkDays);
    }
}
