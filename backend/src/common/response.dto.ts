export class ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;

  static ok<T>(data: T, message?: string): ApiResponse<T> {
    const res = new ApiResponse<T>();
    res.success = true;
    res.data = data;
    res.message = message;
    return res;
  }

  static error(message: string): ApiResponse<null> {
    const res = new ApiResponse<null>();
    res.success = false;
    res.message = message;
    res.data = null;
    return res;
  }
}
