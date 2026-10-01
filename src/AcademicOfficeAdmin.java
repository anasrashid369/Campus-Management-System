public class AcademicOfficeAdmin extends Administrator {

    AcademicOfficeAdmin(String name, String email, String phone, String adminId) {
        super(name, email, phone, adminId);
    }

    // TODO: createCourse(...), updateCourse(...), searchCourse(...)
    // TODO: createSection(...), updateSection(...), setCapacity(...)
    // TODO: assignRoom(...), assignInstructor(...)
    // TODO: viewRequests(), approveRequest(...), rejectRequest(...)

    @Override
    public String getRole() {
        return "Academic Office Admin";
    }
}