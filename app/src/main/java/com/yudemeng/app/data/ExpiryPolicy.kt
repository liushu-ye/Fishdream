package com.yudemeng.app.data

import androidx.annotation.StringRes
import com.yudemeng.app.R

/** 保留时间策略。FOREVER 表示不设置过期时间（expires_at = NULL）。 */
enum class ExpiryPolicy(@StringRes val labelRes: Int) {
    TODAY(R.string.policy_today),
    THREE_DAYS(R.string.policy_three_days),
    ONE_WEEK(R.string.policy_one_week),
    THIS_MONTH(R.string.policy_this_month),
    FOREVER(R.string.policy_forever);

    companion object {
        fun fromKey(key: String): ExpiryPolicy = entries.firstOrNull { it.name == key } ?: FOREVER
    }
}
