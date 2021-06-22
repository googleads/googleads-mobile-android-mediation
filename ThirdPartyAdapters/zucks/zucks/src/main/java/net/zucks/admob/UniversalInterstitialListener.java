package net.zucks.admob;

import androidx.annotation.NonNull;

import net.zucks.listener.AdFullscreenInterstitialListener;
import net.zucks.listener.AdInterstitialListener;

/** Utility for unifies Zucks Interstitial format's event listener. */
public abstract class UniversalInterstitialListener<T> {

  @NonNull protected final Callback callback;

  private UniversalInterstitialListener(@NonNull Callback callback) {
    this.callback = callback;
  }

  /** Return format-specific listener implements. */
  @NonNull
  public abstract T use();

  /** Register this implementation to receive ad events. */
  public interface Callback {

    void onReceiveAd();

    void onShowAd();

    void onCancelDisplayRate();

    void onTapAd();

    void onCloseAd();

    void onLoadFailure(Exception exception);

    void onShowFailure(Exception exception);
  }

  /**
   * For (non-fullscreen) Interstitial format.
   *
   * @see net.zucks.view.AdInterstitial AdInterstitial
   */
  public static class Interstitial extends UniversalInterstitialListener<AdInterstitialListener> {

    private final AdInterstitialListener listener =
        new AdInterstitialListener() {

          @Override
          public void onReceiveAd() {
            callback.onReceiveAd();
          }

          @Override
          public void onShowAd() {
            callback.onShowAd();
          }

          @Override
          public void onCancelDisplayRate() {
            callback.onCancelDisplayRate();
          }

          @Override
          public void onTapAd() {
            callback.onTapAd();
          }

          @Override
          public void onCloseAd() {
            callback.onCloseAd();
          }

          @Override
          public void onLoadFailure(Exception e) {
            callback.onLoadFailure(e);
          }

          @Override
          public void onShowFailure(Exception e) {
            callback.onShowFailure(e);
          }
        };

    public Interstitial(@NonNull Callback callback) {
      super(callback);
    }

    @NonNull
    @Override
    public AdInterstitialListener use() {
      return listener;
    }
  }

  /**
   * For fullscreen Interstitial format.
   *
   * @see net.zucks.view.AdFullscreenInterstitial AdFullscreenInterstitial
   */
  public static class FullscreenInterstitial
      extends UniversalInterstitialListener<AdFullscreenInterstitialListener> {

    private final AdFullscreenInterstitialListener listener =
        new AdFullscreenInterstitialListener() {

          @Override
          public void onShowAd() {
            callback.onShowAd();
          }

          @Override
          public void onCancelDisplayRate() {
            callback.onCancelDisplayRate();
          }

          @Override
          public void onTapAd() {
            callback.onTapAd();
          }

          @Override
          public void onCloseAd() {
            callback.onCloseAd();
          }

          @Override
          public void onLoadFailure(Exception e) {
            callback.onLoadFailure(e);
          }

          @Override
          public void onShowFailure(Exception e) {
            callback.onShowFailure(e);
          }

          @Override
          public void onReceiveAd() {
            callback.onReceiveAd();
          }
        };

    public FullscreenInterstitial(@NonNull Callback callback) {
      super(callback);
    }

    @NonNull
    @Override
    public AdFullscreenInterstitialListener use() {
      return listener;
    }
  }
}
