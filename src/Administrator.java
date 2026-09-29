public abstract class Administrator extends Person{
    private String adminId;

    Administrator(String name,String email,String phone,String adminId){
        super(name,email,phone);
        this.adminId = adminId;
    }

    //Methods
    public String getAdminId(){
        return adminId;
    }
    public  String getRole(){
        return "Administrator";
    }

}
