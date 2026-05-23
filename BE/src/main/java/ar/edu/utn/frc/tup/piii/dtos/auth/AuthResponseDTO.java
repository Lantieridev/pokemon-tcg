package ar.edu.utn.frc.tup.piii.dtos.auth;

public class AuthResponseDTO {
    private String token;
    private String username;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static AuthResponseDTOBuilder builder() {
        return new AuthResponseDTOBuilder();
    }

    public static class AuthResponseDTOBuilder {
        private String token;
        private String username;

        public AuthResponseDTOBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseDTOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuthResponseDTO build() {
            return new AuthResponseDTO(token, username);
        }
    }
}
