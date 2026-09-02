package com.mulaisekarang.app.data

import com.mulaisekarang.app.data.model.CartRequest
import com.mulaisekarang.app.data.model.CheckoutResult
import com.mulaisekarang.app.data.model.CartPreview
import com.mulaisekarang.app.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The cart is client-side by design — the backend keeps no cart state (the web
 * cart is session-based and unusable with token auth). We hold the ids here and
 * post the whole list at preview/checkout; the server is authoritative on price
 * and eligibility.
 *
 * Singleton so the badge and the cart screen agree, and so adding from course
 * detail survives navigation.
 */
@Singleton
class CartRepository @Inject constructor(private val api: ApiService) {

    private val _courseIds = MutableStateFlow<List<Int>>(emptyList())
    val courseIds: StateFlow<List<Int>> = _courseIds.asStateFlow()

    private val _voucherCode = MutableStateFlow<String?>(null)

    /** The API caps a cart at 20 entries; enforce it before the round-trip. */
    fun add(courseId: Int): Boolean {
        if (_courseIds.value.contains(courseId)) return true
        if (_courseIds.value.size >= MAX_ITEMS) return false

        _courseIds.update { it + courseId }
        return true
    }

    fun remove(courseId: Int) = _courseIds.update { ids -> ids.filterNot { it == courseId } }

    fun clear() {
        _courseIds.update { emptyList() }
        _voucherCode.value = null
    }

    fun contains(courseId: Int): Boolean = _courseIds.value.contains(courseId)

    fun setVoucherCode(code: String?) {
        _voucherCode.value = code
    }

    suspend fun preview(): CartPreview =
        api.cartPreview(CartRequest(_courseIds.value, _voucherCode.value)).data

    suspend fun checkout(): CheckoutResult =
        api.cartCheckout(CartRequest(_courseIds.value, _voucherCode.value)).data

    companion object {
        const val MAX_ITEMS = 20
    }
}
