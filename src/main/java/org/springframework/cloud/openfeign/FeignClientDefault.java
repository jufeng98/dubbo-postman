package org.springframework.cloud.openfeign;

import com.rpcpostman.service.AppFactory;
import com.rpcpostman.service.Pair;
import com.rpcpostman.service.invocation.entity.DubboParamValue;
import com.rpcpostman.service.invocation.entity.PostmanDubboRequest;
import com.rpcpostman.service.registry.Register;
import static com.rpcpostman.util.Constant.FEIGN_PARAM;
import com.rpcpostman.util.SpringUtil;
import com.rpcpostman.util.ThreadLocalUtil;
import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.lang3.RandomUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.List;

/**
 * @author yudong
 * @date 2022/11/9
 */
public class FeignClientDefault extends Client.Default {

    public FeignClientDefault() {
        this(null, null);
    }

    public FeignClientDefault(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier) {
        super(sslContextFactory, hostnameVerifier);
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        Pair<PostmanDubboRequest, DubboParamValue> pair = ThreadLocalUtil.get(FEIGN_PARAM);
        String serviceName = pair.getLeft().getServiceName();
        AppFactory appFactory = SpringUtil.getContext().getBean(AppFactory.class);
        Register register = appFactory.getRegisterFactory(pair.getLeft().getCluster()).get(pair.getLeft().getCluster());
        String host;
        if (pair.getRight().isUseDubbo()) {
            host = pair.getRight().getDubboUrl().replace("dubbo://", "");
        } else {
            List<String> instances = register.getServiceInstances(serviceName);
            int i = RandomUtils.nextInt(0, instances.size());
            host = instances.get(i);
        }
        String url = request.url().replace(serviceName.toLowerCase(), host);
        request = Request.create(
                request.httpMethod(),
                url,
                request.headers(),
                request.requestBody()
        );
        return super.execute(request, options);
    }
}
