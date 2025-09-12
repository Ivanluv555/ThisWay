import cv2

cap = cv2.VideoCapture(0)

if not cap.isOpened():
    print("无法打开摄像头")
    exit()

while True:
    ret, frame = cap.read()

    # 如果正确读取帧，ret为True
    if not ret:
        print("无法读取帧 退出 ...")
        break

    # 显示帧
    cv2.imshow('Camera', frame)

    # 按下 'q' 键退出循环
    if cv2.waitKey(1) == ord('q'):
        break

# 关闭所有窗口
cap.release()
cv2.destroyAllWindows()
