import cv2
import numpy as np
import DeepFace
def build_model():
    from retinaface import RetinaFace
    face_detector = RetinaFace.build_model()
    return face_detector

def detect_face(face_detector, img, align=True):
    from retinaface import RetinaFace
    from retinaface.commons import postprocess

    resp = []

    obj = RetinaFace.detect_faces(img, model=face_detector, threshold=0.9)

    if isinstance(obj, dict):
        for face_idx in obj.keys():
            identity = obj[face_idx]
            facial_area = identity["facial_area"]

            y = facial_area[1]
            h = facial_area[3] - y
            x = facial_area[0]
            w = facial_area[2] - x
            img_region = [x, y, w, h]
            confidence = identity["score"]
            detected_face = img[facial_area[1]:facial_area[3], facial_area[0]:facial_area[2]]

            if align:
                landmarks = identity["landmarks"]
                left_eye = landmarks["left_eye"]
                right_eye = landmarks["right_eye"]
                nose = landmarks["nose"]

                detected_face = postprocess.alignment_procedure(detected_face, right_eye, left_eye, nose)

            resp.append((detected_face, img_region, confidence))

    return resp

def extract_faces_retinaface(img_path, target_size=(112, 112), grayscale=False, align=True):
    # Load the image using OpenCV
    img = cv2.imread(img_path)

    # Detect faces using the given RetinaFace functions
    face_detector = build_model()
    face_objs = detect_face(face_detector, img, align=align)

    extracted_faces = []

    for current_img, current_region, confidence in face_objs:
        if grayscale:
            current_img = cv2.cvtColor(current_img, cv2.COLOR_BGR2GRAY)

        current_img = cv2.resize(current_img, target_size)
        img_pixels = np.asarray(current_img) / 255.0
        
        if grayscale:
            img_pixels = np.expand_dims(img_pixels, axis=-1)

        region_obj = {
            "x": int(current_region[0]),
            "y": int(current_region[1]),
            "w": int(current_region[2]),
            "h": int(current_region[3]),
        }

        extracted_faces.append([img_pixels, region_obj, confidence])

    return extracted_faces



face1 = extract_faces_retinaface("C:\\Users\\user\\Desktop\\asd\\1.jpg")[0][0]
face2 = extract_faces_retinaface("C:\\Users\\user\\Desktop\\asd\\2.jpg")[0][0]

# Convert extracted face back to [0, 255] scale and save
cv2.imwrite("face1.jpg", (face1 * 255).astype(np.uint8))
cv2.imwrite("face2.jpg", (face2 * 255).astype(np.uint8))

# Use DeepFace to verify the two extracted face images
result = DeepFace.verify(img1_path = "face1.jpg", img2_path = "face2.jpg", 
                         model_name= "ArcFace",)

print("Are these images of the same person?: ", result["verified"])