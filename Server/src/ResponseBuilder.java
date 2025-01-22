public class ResponseBuilder {
    private String response;
    private String param;
    private String msg;

    public ResponseBuilder() {
        this.response = "CHAT";
        this.param = "";
        this.msg = "";
    }

    public ResponseBuilder putResponse(String response){
        this.response = response;
        return this;
    }

        public ResponseBuilder useRequestErrorResponse(){
            this.response = "REQUEST_ERROR";
            return this;
        }

        public ResponseBuilder useConnectionClosedResponse(){
            this.response = "CONNECTION_CLOSED";
            return this;
        }

    public ResponseBuilder addParam(String param){
        this.param += "&" + param;
        return this;
    }

    public ResponseBuilder putMessage(String msg){
        this.msg = msg;
        return this;
    }

    public String build(){
        String result = "@" + this.response + "%" + this.param + "#" + this.msg;
        String encryptedResult = "@" + AES256.encrypt(result);
        return encryptedResult;
    }
}