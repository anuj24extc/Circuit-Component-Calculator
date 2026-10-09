package labstock.model;

/**
 * The student who borrows an item. Every BorrowTransaction has one.
 */
public class Student {

    private final String name;
    private final String rollNumber;

    public Student(String name, String rollNumber) {
        this.name = name;
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    @Override
    public String toString() {
        return name + " (" + rollNumber + ")";
    }
}
