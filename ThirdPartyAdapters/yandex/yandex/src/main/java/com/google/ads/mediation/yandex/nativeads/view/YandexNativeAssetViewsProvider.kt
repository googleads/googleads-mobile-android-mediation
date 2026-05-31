package com.google.ads.mediation.yandex.nativeads.view

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.yandex.mobile.ads.nativeads.Rating

internal class YandexNativeAssetViewsProvider private constructor(builder: Builder) {

    val ageView: TextView? = builder.ageView

    val bodyView: TextView? = builder.bodyView

    val callToActionView: TextView? = builder.callToActionView

    val domainView: TextView? = builder.domainView

    val faviconView: ImageView? = builder.faviconView

    val feedbackView: ImageView? = builder.feedbackView

    val imageView: ImageView? = builder.imageView

    val iconView: ImageView? = builder.iconView

    val priceView: TextView? = builder.priceView

    private val ratingView: View? = builder.ratingView

    val reviewCountView: TextView? = builder.reviewCountView

    val sponsoredView: TextView? = builder.sponsoredView

    val titleView: TextView? = builder.titleView

    val warningView: TextView? = builder.warningView

    fun <T> getRatingView(): T? where T : View?, T : Rating? {
        return ratingView as T?
    }

    class Builder {

        var ageView: TextView? = null
            private set

        var bodyView: TextView? = null
            private set

        var callToActionView: TextView? = null
            private set

        var domainView: TextView? = null
            private set

        var faviconView: ImageView? = null
            private set

        var feedbackView: ImageView? = null
            private set

        var imageView: ImageView? = null
            private set

        var iconView: ImageView? = null
            private set

        var priceView: TextView? = null
            private set

        var ratingView: View? = null
            private set

        var reviewCountView: TextView? = null
            private set

        var sponsoredView: TextView? = null
            private set

        var titleView: TextView? = null
            private set

        var warningView: TextView? = null
            private set

        fun build(): YandexNativeAssetViewsProvider {
            return YandexNativeAssetViewsProvider(this)
        }

        fun withAgeView(ageView: TextView?): Builder {
            this.ageView = ageView
            return this
        }

        fun withBodyView(bodyView: TextView?): Builder {
            this.bodyView = bodyView
            return this
        }

        fun withCallToActionView(callToActionView: TextView?): Builder {
            this.callToActionView = callToActionView
            return this
        }

        fun withDomainView(domainView: TextView?): Builder {
            this.domainView = domainView
            return this
        }

        fun withFaviconView(faviconView: ImageView?): Builder {
            this.faviconView = faviconView
            return this
        }

        fun withFeedbackView(feedbackView: ImageView?): Builder {
            this.feedbackView = feedbackView
            return this
        }

        fun withIconView(iconView: ImageView?): Builder {
            this.iconView = iconView
            return this
        }

        fun withImageView(imageView: ImageView?): Builder {
            this.imageView = imageView
            return this
        }

        fun withPriceView(priceView: TextView?): Builder {
            this.priceView = priceView
            return this
        }

        fun <T> withRatingView(ratingView: T?): Builder where T : View?, T : Rating? {
            this.ratingView = ratingView
            return this
        }

        fun withReviewCountView(reviewCountView: TextView?): Builder {
            this.reviewCountView = reviewCountView
            return this
        }

        fun withSponsoredView(sponsoredView: TextView?): Builder {
            this.sponsoredView = sponsoredView
            return this
        }

        fun withTitleView(titleView: TextView?): Builder {
            this.titleView = titleView
            return this
        }

        fun withWarningView(warningView: TextView?): Builder {
            this.warningView = warningView
            return this
        }
    }
}
