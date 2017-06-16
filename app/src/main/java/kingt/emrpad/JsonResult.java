package kingt.emrpad;

/**
 * Created by shao on 2017/5/11.
 */

public class JsonResult {
    private Boolean Ok;
    private String Message;

    public JsonResult(){}
    public JsonResult(boolean ok, String message){
        this.Ok = ok;
        this.Message = message;
    }

    public Boolean isOk() {
        return Ok;
    }
    public void setOk(Boolean ok) {
        this.Ok = ok;
    }

    public String getMessage() {
        return Message;
    }
    public void setMessage(String message) {
        this.Message = message;
    }
}
