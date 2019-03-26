package network.omisego.omgwallet.setup.custom.assertions

import androidx.test.espresso.assertion.ViewAssertions
import com.agoda.kakao.common.assertions.BaseAssertions
import network.omisego.omgwallet.setup.custom.matchers.ToolbarMatcher

/*
 * OmiseGO
 *
 * Created by Phuchit Sirimongkolsathien on 22/10/2018 AD.
 * Copyright © 2017-2018 OmiseGO. All rights reserved.
 */

interface ToolbarAssertion : BaseAssertions {
    fun hasTitle(title: String) {
        view.check(ViewAssertions.matches(ToolbarMatcher(title)))
    }
}
