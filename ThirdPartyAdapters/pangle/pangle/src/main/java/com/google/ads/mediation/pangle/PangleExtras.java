package com.google.ads.mediation.pangle;

import android.os.Bundle;

public class PangleExtras {

  /**
   * Class containing keys for the Pangle extras {@link Bundle}.
   */
  static class Keys {

    static final String USER_DATA = "user_data";
  }

  /**
   * Convenience class used to build the Pangle network extras {@link Bundle}.
   */
  public static class Builder {

    private String mUserData;

    /**
     * Use this to set user data.
     */
    public Builder setUserData(String userData) {
      mUserData = userData;
      return this;
    }

    /**
     * Builds a {@link Bundle} object with the given inputs.
     */
    public Bundle build() {
      final Bundle extras = new Bundle();
      extras.putString(Keys.USER_DATA, mUserData);
      return extras;
    }
  }
}
