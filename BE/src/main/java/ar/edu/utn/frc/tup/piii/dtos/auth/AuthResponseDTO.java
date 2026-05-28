package ar.edu.utn.frc.tup.piii.dtos.auth;

public class AuthResponseDTO {
    private String token;
    private String username;
    private Long userId;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, String username, Long userId) {
        this.token = token;
        this.username = username;
        this.userId = userId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public static AuthResponseDTOBuilder builder() {
        return new AuthResponseDTOBuilder();
    }

    public static class AuthResponseDTOBuilder {
        private String token;
        private String username;
        private Long userId;

        public AuthResponseDTOBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseDTOBuilder username(String username) {
            this.username = username;
            return this;
        }

        public AuthResponseDTOBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AuthResponseDTO build() {
            return new AuthResponseDTO(token, username, userId);
        }
    }
}
