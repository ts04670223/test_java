package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.model.Wishlist;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import com.example.demo.service.WishlistService;

@WebMvcTest(controllers = WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
public class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    // GET /api/wishlist

    @Test
    @WithMockUser(username = "testuser")
    public void getMyWishlist_emptyList_returnsSuccessWithZeroCount() throws Exception {
        when(wishlistService.getWishlistByUsername("testuser")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/wishlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.count").value(0));
    }

    // POST /api/wishlist/items

    @Test
    @WithMockUser(username = "testuser")
    public void addToWishlist_success_returnsWishlistItemId() throws Exception {
        Wishlist mockWishlist = mock(Wishlist.class);
        when(mockWishlist.getId()).thenReturn(42L);
        when(wishlistService.addToWishlistByUsername(eq("testuser"), eq(1L))).thenReturn(mockWishlist);

        mockMvc.perform(post("/api/wishlist/items").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.wishlistItemId").value(42));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void addToWishlist_duplicateProduct_returns400() throws Exception {
        when(wishlistService.addToWishlistByUsername(eq("testuser"), eq(1L)))
                .thenThrow(new RuntimeException("duplicate"));

        mockMvc.perform(post("/api/wishlist/items").param("productId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    public void addToWishlist_productNotFound_returns400() throws Exception {
        when(wishlistService.addToWishlistByUsername(eq("testuser"), eq(999L)))
                .thenThrow(new RuntimeException("not found"));

        mockMvc.perform(post("/api/wishlist/items").param("productId", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // DELETE /api/wishlist/items/{productId}

    @Test
    @WithMockUser(username = "testuser")
    public void removeFromWishlist_success_returns200() throws Exception {
        doNothing().when(wishlistService).removeFromWishlistByUsername(eq("testuser"), eq(1L));

        mockMvc.perform(delete("/api/wishlist/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void removeFromWishlist_productNotFound_returns400() throws Exception {
        doThrow(new RuntimeException("not found"))
                .when(wishlistService).removeFromWishlistByUsername(eq("testuser"), eq(999L));

        mockMvc.perform(delete("/api/wishlist/items/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // GET /api/wishlist/check

    @Test
    @WithMockUser(username = "testuser")
    public void checkWishlist_productIsInWishlist_returnsTrue() throws Exception {
        when(wishlistService.isInWishlistByUsername(eq("testuser"), eq(1L))).thenReturn(true);

        mockMvc.perform(get("/api/wishlist/check").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.inWishlist").value(true));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void checkWishlist_productNotInWishlist_returnsFalse() throws Exception {
        when(wishlistService.isInWishlistByUsername(eq("testuser"), eq(2L))).thenReturn(false);

        mockMvc.perform(get("/api/wishlist/check").param("productId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.inWishlist").value(false));
    }

    // DELETE /api/wishlist/items  -- NEW: clear all wishlist items
    // RED PHASE: endpoint not yet implemented -> expected 200, actual 404/500 -> FAIL
    @Test
    @WithMockUser(username = "testuser")
    public void clearWishlist_success_returns200() throws Exception {
        mockMvc.perform(delete("/api/wishlist/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}