package net.foxopen.fox;

import org.apache.commons.httpclient.Header;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public abstract class FoxResponse {
  private final List<Header> mHttpHeaderList = new ArrayList<>();
  private final List<BeforeResponseAction> mBeforeResponseActions = new ArrayList<>();

  protected int mStatusCode = HttpServletResponse.SC_OK;

  /**
   * Sets HTTP headers on a response based on whats contained in the header list
   */
  protected void setResponseHttpHeaders(HttpServletResponse pResponse) {
    for(Header lHeader : mHttpHeaderList) {
      // Overwrites a previous header with the same name. Use addHeader to allow multiple values
      pResponse.setHeader(lHeader.getName(), lHeader.getValue());
    }
  }


  protected List getHttpHeaderList() {
    return mHttpHeaderList;
  }

  /**
   * Adds an HTTP header to a local list. The headers are not applied to a response
   * until setResponseHttpHeaders is called
   */
  public void setHttpHeader(String pName, String pValue) {
    mHttpHeaderList.add(new Header(pName, pValue));
  }

  public void setStatus(int pStatusCode) {
    mStatusCode = pStatusCode;
  }

  public int getStatusCode() {
    return mStatusCode;
  }

  public abstract void respond(FoxRequest pRequest);

  /**
   * Registers a BeforeResponseAction which will be invoked before this response is sent.
   * @param pAction Action to register.
   */
  public void addBeforeResponseAction(BeforeResponseAction pAction) {
    mBeforeResponseActions.add(pAction);
  }

  /**
   * Executes all BeforeResponseActions registered on this FoxResponse in the order they were added. Consumers MUST
   * call this before any response is sent.
   */
  protected void runBeforeResponseActions() {
    mBeforeResponseActions.forEach(e -> e.beforeResponse(this));
  }

  /**
   * Actions which should be performed before a response starts to be sent to the user, i.e. JIT setting of headers.
   */
  public interface BeforeResponseAction {
    void beforeResponse(FoxResponse pFoxResponse);
  }
}
