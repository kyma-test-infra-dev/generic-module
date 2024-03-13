package com.sap.cloud.connectivity.validating.admission.controller.validation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.connectivity.validating.admission.controller.validation.service.ConnectivityProxyValidator;

import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;

@RestController
public class ValidatingAdmissionController {

	private final ConnectivityProxyValidator connectivityProxyValidator;

	@Autowired
	public ValidatingAdmissionController(ConnectivityProxyValidator connectivityProxyValidator) {
		this.connectivityProxyValidator = connectivityProxyValidator;
	}

	@PostMapping("/validate")
	@ResponseBody
	public AdmissionReview validate(@RequestBody AdmissionReview admissionReview) {
		return connectivityProxyValidator.validate(admissionReview);
	}

}
