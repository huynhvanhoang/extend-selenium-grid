package selenium.extend.hub.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openqa.grid.common.exception.GridException;
import org.openqa.grid.internal.GridRegistry;
import org.openqa.grid.internal.ProxySet;
import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.seleniumhq.jetty9.util.StringUtil;

public class NodeInfo extends RegistryBasedServlet {

  public static HashMap<String, Long> nodeReserve = new HashMap<String, Long>();
  private static Gson gson = new Gson();

  private String BUSY_NODES = "busyNodes";
  private String FREE_NODES = "freeNodes";

  public NodeInfo() {
    this(null);
  }

  public NodeInfo(GridRegistry registry) {
    super(registry);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    process(request, response);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    process(request, response);
  }

  protected void process(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setStatus(200);

    try {
      JsonObject res = getNodes(request);
      response.getWriter().print(res);
      response.getWriter().close();
    } catch (JsonSyntaxException e) {
      throw new GridException(e.getMessage());
    }
  }

  private JsonObject getNodes(HttpServletRequest request) {
    String nodeStatus = request.getParameter("nodeStatus");
    int count = 10;
    if (StringUtil.isNotBlank(request.getParameter("count"))) {
      count = Integer.parseInt(request.getParameter("count").trim());
    }

    System.out.println("request info: nodestatus " + nodeStatus + " count: " + count);
    System.out.println("list reserveNode " + gson.toJson(nodeReserve));

    JsonObject responseObject = new JsonObject();
    JsonArray freeProxies = new JsonArray();
    JsonArray busyProxies = new JsonArray();

    ProxySet proxies = super.getRegistry().getAllProxies();
    Iterator<RemoteProxy> itr = proxies.iterator();
    JsonObject listNode = getListNode(itr);
    System.out.println("list node " + gson.toJson(listNode));
    if (StringUtil.isBlank(nodeStatus)) {
      return listNode;
    } else if ("free".equals(nodeStatus)) {
      JsonArray jsonArray = (JsonArray) listNode.get(FREE_NODES);
      System.out.println("list node free" + gson.toJson(jsonArray));
      getFreeAndBusyNode(count, freeProxies, jsonArray);
    } else {
      busyProxies = (JsonArray) listNode.get(BUSY_NODES);
    }
    responseObject.add(FREE_NODES, freeProxies);
    responseObject.add(BUSY_NODES, busyProxies);
    return responseObject;
  }

  private void getFreeAndBusyNode(int count, JsonArray freeProxies, JsonArray jsonArray) {
    for (int i = 0; i < jsonArray.size(); i++) {
      String udid = jsonArray.get(i).getAsJsonObject().get("udid").toString();
      System.out.println("udid " + udid);
      if (nodeReserve.size() > 0 && nodeReserve.containsKey(udid)) {
        Long timeoutStamp = nodeReserve.get(udid) + TimeUnit.MINUTES.toMillis(1);
        System.out.println(
            "currentTimestamp " + System.currentTimeMillis() + " timestamp node reserve "
                + timeoutStamp);
        if (System.currentTimeMillis() > timeoutStamp) {
          freeProxies.add(jsonArray.get(i));
          nodeReserve.put(udid, System.currentTimeMillis());
          System.out.println("add node " + freeProxies);
        }
      } else {
        freeProxies.add(jsonArray.get(i));
        nodeReserve.put(udid, System.currentTimeMillis());
        System.out.println("add node with node reserve empty" + freeProxies);
      }
      if (freeProxies.size() >= count) {
        break;
      }
    }
  }

  private JsonObject getListNode(Iterator<RemoteProxy> itr) {
    JsonArray freeProxies = new JsonArray();
    JsonArray busyProxies = new JsonArray();
    String nodeName = "";
    String udid = "";
    String platform = "";
    String browserName = "";
    while (itr.hasNext()) {

      RemoteProxy proxy = itr.next();
      JsonObject proxyJson = new JsonObject();
//      nodeName = proxy.getOriginalRegistrationRequest().getConfiguration().capabilities
//          .get(0).asMap().get("nodeName").toString();
      udid = proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0).asMap()
          .get("udid").toString();
      platform = proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0)
          .asMap().get("platform").toString();
      browserName = proxy.getOriginalRegistrationRequest().getConfiguration().capabilities.get(0)
          .asMap().get("browserName").toString();

     // proxyJson.add("nodeName", gson.toJsonTree(nodeName));
      proxyJson.add("udid", gson.toJsonTree(udid));
      proxyJson.add("platform", gson.toJsonTree(platform));
      proxyJson.add("browserName", gson.toJsonTree(browserName));

      if (!proxy.isBusy()) {
        freeProxies.add(proxyJson);
      } else {
        busyProxies.add(proxyJson);
      }
    }

    JsonObject nodeJson = new JsonObject();
    nodeJson.add(FREE_NODES, freeProxies);
    nodeJson.add(BUSY_NODES, busyProxies);

    return nodeJson;
  }
}
