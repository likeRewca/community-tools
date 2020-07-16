package com.community.tools.service.github;

import com.community.tools.service.slack.SlackService;
import com.community.tools.util.GithubAuthChecker;
import com.github.seratch.jslack.api.methods.SlackApiException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

@Service
public class GitHubHookServlet extends HttpServlet {

  @Value("${spring.datasource.url}")
  private String url;
  @Value("${spring.datasource.username}")
  private String username;
  @Value("${spring.datasource.password}")
  private String password;
  @Value("${GITHUB_SECRET_TOKEN}")
  private String secret;
  @Autowired
  private SlackService service;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    StringBuilder builder = new StringBuilder();
    String aux = "";

    while ((aux = req.getReader().readLine()) != null) {
      builder.append(aux);
    }
    JSONObject json = new JSONObject(builder.toString());

    try {
      if (new GithubAuthChecker(secret)
          .checkSignature(req.getHeader("X-Hub-Signature"), builder.toString())) {

        SingleConnectionDataSource connect = new SingleConnectionDataSource();
        connect.setUrl(url);
        connect.setUsername(username);
        connect.setPassword(password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(connect);

        if (json.get("action").toString().equals("opened")) {
          JSONObject pull = json.getJSONObject("pull_request");
          String user = pull.getJSONObject("user").getString("login");
          String url = pull.getJSONObject("_links").getJSONObject("html").getString("href");
          service
              .sendMessageToChat("test", "User" + user + " create a pull request \n url: " + url);
        }

        jdbcTemplate.update(
            "INSERT INTO public.\"GitHookData\" (time, jsonb_data) VALUES ('" + new Date() + "','"
                + json + "'::jsonb);");
      }
    } catch (NoSuchAlgorithmException | InvalidKeyException | SlackApiException e) {
      throw new RuntimeException(e);
    }

  }
}
