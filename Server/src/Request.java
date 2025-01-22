import java.util.Arrays;

//TODO refactor & добавить params hashmap
public class Request {
    public final String head;
    public final String[] params;
    public final String msg;

    public Request (String request){
        String requestDecrypted = AES256.decrypt(request);
        msg = getMsgSave(requestDecrypted);
        head = getHeadSave(requestDecrypted);
        params = getParamsSave(requestDecrypted);
    }

    private String getHeadSave(String headString) {
        String hString = headString;
        if (hString.contains("%") && hString.length() > 1) {
            return hString.split("%")[0].replace("@", "");
        }
        return "";
    }

    private String getMsgSave(String msgString) {
        String mString = msgString;
        if(mString.contains("#") && mString.length() > 1){
            String[] res = msgString.split("#");

            if (res.length > 1) {
                return msgString.split("#")[1];
            }
        }
        return "";
    }

    private String[] getParamsSave(String paramString) {
        String pString = paramString;
        if(pString.contains("%") && pString.length() > 1) {
            String params_msg = pString.split("%")[1];

            if(params_msg.contains("#") && params_msg.length() > 1) {
                String paramsString = params_msg.split("#")[0];

                if(paramsString.contains("&") && paramsString.length() > 1) {
                    String[] paramsArray = paramsString.split("&");

                    if(paramsArray.length > 1){
                        return Arrays.copyOfRange(paramsArray, 1 , paramsArray.length);
                    }
                }
            }
        }
        return new String[]{""};
    }
}