/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.cla.mvc.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import io.pivotal.cla.data.AccessToken;
import io.pivotal.cla.data.ContributorLicenseAgreement;
import io.pivotal.cla.data.MarkdownContent;
import io.pivotal.cla.data.User;
import io.pivotal.cla.data.repository.AccessTokenRepository;
import io.pivotal.cla.data.repository.ContributorLicenseAgreementRepository;
import io.pivotal.cla.mvc.util.UrlBuilder;
import io.pivotal.cla.service.ContributingUrlsResponse;
import io.pivotal.cla.service.CreatePullRequestHookRequest;
import io.pivotal.cla.service.GitHubService;

@Controller
public class AdminClaController {

	/**
	 *
	 */
	private static final String ACCESS_TOKENS_URL = "https://github.com/settings/applications";

	@Autowired
	GitHubService github;

	@Autowired
	ContributorLicenseAgreementRepository claRepo;

	@Autowired
	AccessTokenRepository tokenRepo;

	@RequestMapping("/admin/cla/")
	public String listClas(Map<String, Object> model) throws Exception {
		model.put("clas", findAllClas());
		return "admin/cla/index";
	}

	@RequestMapping("/admin/cla/create")
	public String createClaForm(Map<String, Object> model) throws Exception {
		model.put("contributorLicenseAgreement", new ContributorLicenseAgreement());
		return "admin/cla/create";
	}

	@RequestMapping(value = "/admin/cla/create", method = RequestMethod.POST)
	public String createCla(@AuthenticationPrincipal User user, @Valid ContributorLicenseAgreement contributorLicenseAgreement, BindingResult result)
			throws Exception {
		if (result.hasErrors()) {
			return "admin/cla/create";
		}

		String accessToken = user.getAccessToken();

		MarkdownContent individual = contributorLicenseAgreement.getIndividualContent();
		String individualHtml = github.markdownToHtml(accessToken, individual.getMarkdown());
		individual.setHtml(individualHtml);

		MarkdownContent corporate = contributorLicenseAgreement.getCorporateContent();
		String corperateHtml = github.markdownToHtml(accessToken, corporate.getMarkdown());
		corporate.setHtml(corperateHtml);

		claRepo.save(contributorLicenseAgreement);
		return "redirect:/admin/cla/?success";
	}

	@RequestMapping("/admin/cla/link")
	public String linkClaForm(@AuthenticationPrincipal User user, Map<String, Object> model) throws Exception {
		model.put("linkClaForm", new LinkClaForm());
		model.put("licenses", findPrimaryClas());
		model.put("accessTokensUrl", ACCESS_TOKENS_URL);
		return "admin/cla/link";
	}

	@ResponseBody
	@RequestMapping("/admin/cla/link/repositories.json")
	public List<String> repositories(@AuthenticationPrincipal User user) throws Exception {
		return github.findRepositoryNames(user.getAccessToken());
	}

	@RequestMapping(value = "/admin/cla/link", method = RequestMethod.POST)
	public String linkCla(@AuthenticationPrincipal User user, HttpServletRequest request, Map<String, Object> model, @Valid LinkClaForm linkClaForm,
			BindingResult result, RedirectAttributes attrs) throws Exception {
		if (result.hasErrors()) {
			model.put("licenses", findPrimaryClas());
			model.put("accessTokensUrl", ACCESS_TOKENS_URL);
			return "admin/cla/link";
		}

		AccessToken accessToken = tokenRepo.findOne(AccessToken.CLA_ACCESS_TOKEN_ID);

		UrlBuilder pullRequestUrlBldr = UrlBuilder.fromRequest(request);
		UrlBuilder signClaUrlBldr = UrlBuilder.fromRequest(request);

		String pullRequestHookUrl = pullRequestUrlBldr
				.path("/github/hooks/pull_request/" + linkClaForm.getClaName())
				.build();
		String signClaUrl = signClaUrlBldr.path("/sign/" + linkClaForm.getClaName()).build();

		CreatePullRequestHookRequest createPullRequest = new CreatePullRequestHookRequest();
		createPullRequest.setAccessToken(user.getAccessToken());
		List<String> repositoryIds = linkClaForm.getRepositories();
		createPullRequest.setRepositoryIds(repositoryIds);
		createPullRequest.setGithubEventUrl(pullRequestHookUrl);
		createPullRequest.setSecret(accessToken.getToken());

		ContributingUrlsResponse contributingUrls = github.getContributingUrls(repositoryIds);
		List<String> hookUrls = github.createPullRequestHooks(createPullRequest);
		attrs.addFlashAttribute("signClaUrl", signClaUrl);
		attrs.addFlashAttribute("hookUrls", hookUrls);
		attrs.addFlashAttribute("success", true);
		attrs.addFlashAttribute("editContributingAdocUrls", contributingUrls.getAsciidoc());
		attrs.addFlashAttribute("editContributingMdUrls", contributingUrls.getMarkdown());

		return "redirect:/admin/cla/link";
	}

	private List<ContributorLicenseAgreement> findPrimaryClas() {
		List<ContributorLicenseAgreement> result = claRepo.findByPrimaryTrue();
		return sort(result);
	}

	private List<ContributorLicenseAgreement> findAllClas() {
		List<ContributorLicenseAgreement> result = new ArrayList<>();
		claRepo.findAll().forEach(result::add);
		return sort(result);
	}

	private List<ContributorLicenseAgreement> sort(List<ContributorLicenseAgreement> result) {
		Collections.sort(result, (lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));
		return result;
	}
}