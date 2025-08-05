/*
    This file is part of Tool Provider Manager - Manager of LTI Tool Providers
    for learning platforms.
    Copyright (C) 2022  Francisco José Fernández Jiménez.

    Tool Provider Manager is free software: you can redistribute it and/or
    modify it under the terms of the GNU General Public License as published
    by the Free Software Foundation, either version 3 of the License, or (at
    your option) any later version.

    Tool Provider Manager is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
    Public License for more details.

    You should have received a copy of the GNU General Public License along
    with Tool Provider Manager. If not, see <https://www.gnu.org/licenses/>.
*/

package es.us.dit.lti.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import es.us.dit.lti.OutcomeService;
import es.us.dit.lti.ToolSession;
import es.us.dit.lti.entity.Attempt;
import es.us.dit.lti.entity.ResourceLink;
import es.us.dit.lti.entity.ResourceUser;
import es.us.dit.lti.persistence.ToolAttemptDao;
import es.us.dit.lti.persistence.ToolResourceLinkDao;
import es.us.dit.lti.persistence.ToolResourceUserDao;

/**
 * Servlet implementation class to read scores/outcomes.
 *
 * @author Francisco José Fernández Jiménez
 */
@WebServlet({ "/learner/readscore", "/instructor/readscore" })
public class ReadScoreServlet extends HttpServlet {
	/**
	 * Serializable requirement.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ReadScoreServlet.class);

	/**
	 * Processes GET request to read current user scores/outcomes.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Used by LTI users to read their scores.
		final HttpSession session = request.getSession();
		final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
		if (ts != null && ts.isOutcomeAllowed()) {
			response.setContentType("text/plain");
			try {
				String score = getScore(ts);
				response.getWriter().append(formatScore(score));
			} catch (final IOException e) {
				// ignore
				logger.error("Get IO Error.", e);
			}

		}
	}

	/**
	 * Gets the score of the current user.
	 *
	 * @param ts tool session
	 * @return the score as a string
	 */
	private String getScore(ToolSession ts) {
		return OutcomeService.readOutcome(ts.getLtiResourceUser(), ts.getToolKey());
	}
	
	/**
	 * Format score.
	 * 
	 * @param score original value
	 * @return score formatted
	 */
	private String formatScore(String score) {
		if (score != null) {
			try {
				final float fScore = Float.parseFloat(score);
				score = String.format("%.1f", fScore * 10);
			} catch (final NumberFormatException e) {
				// ignore
				logger.error("Not float: {}", score);
			}
		} else {
			score = "null";
		}
		return score;
	}

	/**
	 * Processes POST request to read other LTI user scores.
	 *
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Only instructors.
		final String cipheredSid = request.getParameter("attemptId");
		final boolean original = request.getParameter("original") != null;
		final HttpSession session = request.getSession();
		final ToolSession ts = (ToolSession) session.getAttribute(ToolSession.class.getName());
		if (ts != null && cipheredSid != null && request.getServletPath().equals("/instructor/readscore")
				&& ts.getTool().getToolUiConfig().isManageAttempts() && ts.isOutcomeAllowed()) {
			final Attempt attempt = ToolAttemptDao.getBySecuredSid(cipheredSid, ts.getToolKey());
			boolean outcomeEnabled = false;
			ResourceUser resUser = null;
			if (attempt != null) {
				if (original) {
					resUser = attempt.getOriginalResourceUser();
				} else {
					resUser = attempt.getResourceUser();
				}
				// Complete fields
				resUser = ToolResourceUserDao.getBySid(resUser.getSid());
				if (resUser.getResultSourceId() != null && !resUser.getResultSourceId().isEmpty()) {
					outcomeEnabled = true;
				}
			}
			final List<ScoreInfo> scores = new ArrayList<>();
			if (outcomeEnabled) {
				final ResourceLink rl = ToolResourceLinkDao.getBySid(resUser.getResourceLink().getSid());
				resUser.setResourceLink(rl);
				if (rl != null && rl.getOutcomeServiceUrl() != null && !rl.getOutcomeServiceUrl().isEmpty()) {
					// Check tk so avoid attempt sid reuse from different tk
					final ScoreInfo info = new ScoreInfo();
					info.setScore(formatScore(OutcomeService.readOutcome(resUser, ts.getToolKey())));
					info.setResourceTitle(resUser.getResourceLink().getTitle());
					info.setContextTitle("");
					scores.add(info);
				}
			}
			// Send result in JSON
			try {
				response.getWriter().append(new Gson().toJson(scores));
			} catch (final IOException e) {
				logger.error("IO Error.", e);
			}
		} else {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			try {
				request.getRequestDispatcher("/errorlogin.html").include(request, response);
			} catch (ServletException | IOException e) {
				logger.error("IO Error.", e);
			}

		}
	}

}
