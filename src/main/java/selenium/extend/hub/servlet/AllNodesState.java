package selenium.extend.hub.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Iterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.web.servlet.RegistryBasedServlet;

public class AllNodesState extends RegistryBasedServlet {

  public AllNodesState() {
    this(null);
  }

  public AllNodesState(GridRegistry registry) {
    super(registry);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    System.out.println("request getRequestURI " + request.getRequestURI());
    System.out.println("request getQueryString " + request.getQueryString());
    System.out.println("request status " + request.getParameter("status"));
    System.out.println("request status " + request.getParameter("count"));

    process(request, response);
  }

  protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(200);

    try {
      JsonObject res = getResponse(request);
      response.getWriter().print(res);
      response.getWriter().close();
    } catch (JsonSyntaxException e) {
      throw new GridException(e.getMessage());
    }
  }

  private JsonObject getResponse(HttpServletRequest request) {
    ProxySet proxies = super.getRegistry().getAllProxies();
    return getNodes(proxies);
  }

  private JsonObject getNodes(ProxySet proxies) {
    Iterator<RemoteProxy> itr = proxies.iterator();
    Gson gson = new Gson();

    JsonArray freeProxies = new JsonArray();
    JsonArray busyProxies = new JsonArray();

    while (itr.hasNext()) {
      RemoteProxy proxy = itr.next();
      JsonObject proxyJson = new JsonObject();
      proxyJson.add("nodeName", gson.toJsonTree(proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0).asMap().get("nodeName")));
      proxyJson.add("udid", gson.toJsonTree(proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0).asMap().get("udid")));
      proxyJson.add("platform", gson.toJsonTree(proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0).asMap().get("platform")));
      proxyJson.add("browserName", gson.toJsonTree(proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0).asMap().get("browserName")));
      System.out.println("node info" + proxy.getOriginalRegistrationRequest().getConfiguration().capabilities);
      if (!proxy.isBusy()) {
        freeProxies.add(proxyJson);
      } else {
        busyProxies.add(proxyJson);
      }
    }

    JsonObject nodeJson = new JsonObject();
    nodeJson.add("freeNodes", freeProxies);
    nodeJson.add("busyNodes", busyProxies);

    return nodeJson;
  }
}
