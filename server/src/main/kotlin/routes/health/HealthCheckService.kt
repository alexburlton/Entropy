package routes.health

import http.dto.HealthCheckResponse
import util.OnlineConstants

class HealthCheckService {
    fun healthCheck() = HealthCheckResponse(OnlineConstants.SERVER_VERSION)
}
