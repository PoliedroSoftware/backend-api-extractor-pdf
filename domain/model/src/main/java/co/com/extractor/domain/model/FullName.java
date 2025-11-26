package co.com.extractor.domain.model;

public class FullName {
    private String firstName;
    private String middleNames;
    private String lastName;
    private String secondLastName;
    private String display;

    public FullName() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleNames() { return middleNames; }
    public void setMiddleNames(String middleNames) { this.middleNames = middleNames; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getSecondLastName() { return secondLastName; }
    public void setSecondLastName(String secondLastName) { this.secondLastName = secondLastName; }
    public String getDisplay() { return display; }
    public void setDisplay(String display) { this.display = display; }
}

