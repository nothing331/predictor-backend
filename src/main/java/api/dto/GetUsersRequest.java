package api.dto;

public class GetUsersRequest {
    private String userId;

    public GetUsersRequest() {
    }

    public GetUsersRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
