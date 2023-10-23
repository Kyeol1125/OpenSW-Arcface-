import sys
import os
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
import boto3
import json
from io import BytesIO
from PIL import Image
import numpy as np
import tensorflow as tf



if os.path.exists("/opt/ml/model/arcface_weights.h5"):
    model_dir = '/opt/ml/model'
else:
    model_dir = '/opt/ml/'
sys.path.append(model_dir)

from deepface import DeepFace
from commons import functions, distance as dst
import ArcFace

print("Num GPUs Available: ", len(tf.config.experimental.list_physical_devices('GPU')))

target_size = 112, 112
detector_backend = 'retinaface'
metric = 'cosine'
boto3.setup_default_session(region_name='ap-northeast-2')
s3_client = boto3.client('s3')

def model_fn(model_dir):
    """Load the model and weights from the given directory."""
    model = ArcFace.loadModel()
    weight_path = os.path.join(model_dir, "arcface_weights.h5")
    model.load_weights(weight_path)
    return model

def input_fn(request_body, request_content_type):
    """Load the input data and fetch images from S3."""
    input_data = json.loads(request_body)
    bucket = 's3-driver-upload'
    img1_path = input_data['img1_path']
    img2_path = input_data['img2_path']

    # Load image directly from S3 to memory
    img1_object = s3_client.get_object(Bucket=bucket, Key=img1_path)
    img1 = Image.open(BytesIO(img1_object['Body'].read()))

    img2_object = s3_client.get_object(Bucket=bucket, Key=img2_path)
    img2 = Image.open(BytesIO(img2_object['Body'].read()))

    return img1, img2

def predict_fn(input_data, model):
    """Predict the similarity between two images."""
    img1, img2 = input_data
    img1_array = np.asarray(img1)
    img2_array = np.asarray(img2)

    img1_faces = functions.extract_faces(img1_array, target_size=target_size, detector_backend=detector_backend)
    img2_faces = functions.extract_faces(img2_array, target_size=target_size, detector_backend=detector_backend)
    
    img1_processed = np.array([face_img for face_img, _, _ in img1_faces])
    img2_processed = np.array([face_img for face_img, _, _ in img2_faces])
    
    img1_processed = np.squeeze(img1_processed, axis=1)
    img2_processed = np.squeeze(img2_processed, axis=1)

    img1_embedding = model.predict(img1_processed)[0]
    img2_embedding = model.predict(img2_processed)[0]

    if metric == 'cosine':
        distance = dst.findCosineDistance(img1_embedding, img2_embedding)
    elif metric == 'euclidean':
        distance = dst.findEuclideanDistance(img1_embedding, img2_embedding)
    elif metric == 'euclidean_l2':
        distance = dst.findEuclideanDistance(dst.l2_normalize(img1_embedding), dst.l2_normalize(img2_embedding))

    threshold = findThreshold(metric)

    if distance <= threshold:
        result = "they are same person"
    else:
        result = "they are different persons"

    return {
        "result": result,
        "distance": round(distance, 2),
        "threshold": round(threshold, 2)
    }
def output_fn(prediction_output, response_content_type):
    """Format the prediction output."""
    if response_content_type == 'application/json':
        return json.dumps(prediction_output)
    else:
        raise ValueError(f"Unsupported content type: {response_content_type}")
    
def findThreshold(metric):
    """Return the appropriate threshold for the given metric."""
    if metric == 'cosine':
        return 0.6871912959056619
    elif metric == 'euclidean':
        return 4.1591468986978075
    elif metric == 'euclidean_l2':
        return 1.1315718048269017
    
    
from flask import Flask, request, jsonify

app = Flask(__name__)
model = model_fn(model_dir)  # 모델을 전역 변수로 로드

@app.route('/ping', methods=['GET'])
def ping():
    return "Alive", 200

@app.route('/invocations', methods=['POST'])
def infer():
    body = request.data.decode('utf-8')
    img1, img2 = input_fn(body, request_content_type='application/json')
    predictions = predict_fn((img1, img2), model)
    return jsonify(predictions)
