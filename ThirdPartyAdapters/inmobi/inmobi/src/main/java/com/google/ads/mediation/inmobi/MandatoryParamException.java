package com.google.ads.mediation.inmobi;

/**
 * An {@link Exception} which is thrown when a mandatory param is not present.
 */
class MandatoryParamException extends Exception {

  public MandatoryParamException(String msg) {
    super(msg);
  }
}
