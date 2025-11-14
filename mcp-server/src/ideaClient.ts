// mcp-server/src/ideaClient.ts
import axios, { AxiosInstance, AxiosError } from 'axios';

/**
 * IDEA Plugin HTTP 客户端
 * 负责与 IDEA Plugin 的 HTTP API 通信
 */
export class IdeaClient {
  private client: AxiosInstance;

  constructor(baseUrl: string = 'http://localhost:58888') {
    this.client = axios.create({
      baseURL: baseUrl,
      timeout: 30000, // 30 秒超时
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // 响应拦截器
    this.client.interceptors.response.use(
      response => response,
      error => {
        this.handleError(error);
        throw error;
      }
    );
  }

  /**
   * 发送 POST 请求到 IDEA Plugin
   */
  async post<T = any>(endpoint: string, data: any): Promise<T> {
    try {
      console.error(`[DEBUG] POST ${endpoint}`);
      console.error(`[DEBUG] Request data:`, JSON.stringify(data, null, 2));

      const response = await this.client.post<T>(endpoint, data);

      console.error(`[DEBUG] Response:`, JSON.stringify(response.data, null, 2));
      return response.data;
    } catch (error) {
      console.error(`[ERROR] POST ${endpoint} failed:`, error);
      throw error;
    }
  }

  /**
   * 发送 GET 请求到 IDEA Plugin
   */
  async get<T = any>(endpoint: string): Promise<T> {
    try {
      console.error(`[DEBUG] GET ${endpoint}`);

      const response = await this.client.get<T>(endpoint);

      console.error(`[DEBUG] Response:`, JSON.stringify(response.data, null, 2));
      return response.data;
    } catch (error) {
      console.error(`[ERROR] GET ${endpoint} failed:`, error);
      throw error;
    }
  }

  /**
   * 发送 DELETE 请求到 IDEA Plugin
   */
  async delete<T = any>(endpoint: string): Promise<T> {
    try {
      console.error(`[DEBUG] DELETE ${endpoint}`);

      const response = await this.client.delete<T>(endpoint);

      console.error(`[DEBUG] Response:`, JSON.stringify(response.data, null, 2));
      return response.data;
    } catch (error) {
      console.error(`[ERROR] DELETE ${endpoint} failed:`, error);
      throw error;
    }
  }

  /**
   * 健康检查
   * @returns 健康检查响应
   */
  async healthCheck(): Promise<any> {
    return this.get('/api/v1/health');
  }

  /**
   * 获取项目信息
   * @returns 项目信息
   */
  async getProjectInfo(): Promise<any> {
    return this.get('/api/v1/project/info');
  }

  /**
   * 等待索引就绪
   * @param timeout 超时时间(毫秒)
   */
  async waitForIndex(timeout: number = 60000): Promise<void> {
    const start = Date.now();
    console.error('[INFO] Waiting for IDEA index to be ready...');

    while (Date.now() - start < timeout) {
      try {
        const health = await this.healthCheck();
        if (health.data && health.data.indexReady) {
          console.error('[INFO] IDEA index is ready');
          return;
        }
      } catch {
        // 忽略错误,继续等待
      }

      await new Promise(resolve => setTimeout(resolve, 1000));
    }

    throw new Error('Timeout waiting for index to be ready');
  }

  /**
   * 错误处理
   */
  private handleError(error: AxiosError): void {
    if (error.response) {
      // HTTP 错误响应 (4xx, 5xx)
      console.error(`[ERROR] HTTP ${error.response.status}:`, error.response.data);
      console.error(`[ERROR] Request URL: ${error.config?.url}`);
    } else if (error.request) {
      // 请求发送但无响应
      console.error('[ERROR] No response from IDEA Plugin');
      console.error('[ERROR] Make sure IDEA Plugin is running on port 58888');
      console.error('[ERROR] Check if IDEA is open and the plugin is active');
    } else {
      // 其他错误
      console.error('[ERROR] Request failed:', error.message);
    }
  }
}
