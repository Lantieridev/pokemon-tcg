package ar.edu.utn.frc.tup.piii.dtos.auth;

public class AuthLoginRequestDTO {
    private String username;
    private String password;

    public AuthLoginRequestDTO() {
    }

    public AuthLoginRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
