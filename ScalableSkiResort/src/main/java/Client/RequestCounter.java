package Client;

public class RequestCounter {

  private int successCount;
  private int failCount;

  public RequestCounter() {
    this.successCount = 0;
    this.failCount = 0;
  }

  synchronized public void incSuccess(int successCount) {
    this.successCount += successCount;
  }

  synchronized public void incFail(int failCount) {
    this.failCount += failCount;
  }

  synchronized public int getSuccessCount() {
    return this.successCount;
  }

  synchronized public int getFailCount() {
    return this.failCount;
  }
}
