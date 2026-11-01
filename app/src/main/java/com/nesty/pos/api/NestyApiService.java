package com.nesty.pos.api;

import com.nesty.pos.api.models.AuthModels;
import com.nesty.pos.api.models.OrderModels;
import com.nesty.pos.api.models.PosStatusResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface NestyApiService {

    @GET("api/pos/status")
    Call<PosStatusResponse> getStatus();

    @POST("api/pos/auth/login")
    Call<AuthModels.LoginResponse> login(@Body AuthModels.LoginRequest request);

    @POST("api/pos/verify-order")
    Call<OrderModels.VerifyResponse> verifyOrder(@Body OrderModels.VerifyRequest request);

    @POST("api/pos/claim-order")
    Call<OrderModels.GenericResponse> claimOrder(@Body OrderModels.ClaimRequest request);

    @POST("api/pos/confirm-print")
    Call<OrderModels.GenericResponse> confirmPrint(@Body OrderModels.ConfirmPrintRequest request);

    @POST("api/pos/report-print-failure")
    Call<OrderModels.GenericResponse> reportFailure(@Body OrderModels.PrintFailureRequest request);
}
