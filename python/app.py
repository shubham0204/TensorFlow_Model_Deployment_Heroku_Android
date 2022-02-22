
from flask import Flask , jsonify , request
from PIL import Image
import tensorflow as tf
import numpy as np
import base64
import io

# Loading the Keras model to perform inference
# Download the model from this release ->
# https://github.com/shubham0204/Age-Gender_Estimation_TF-Android/releases/tag/v1.0
model = tf.keras.models.load_model( 'models/model_age.h5' )

# Initialize the Flask app
app = Flask( __name__ )

# Run this method when a request is made, through POST method
@app.route( "/predict" , methods=[ 'POST' ] )
def predict():
    # Get the base64 encoding of the image
    image_base64 = request.get_json()[ 'image' ]
    # Decode the image from the base64 encoding.
    # Refer to this SO answer -> https://stackoverflow.com/a/59007838/13546426
    image = Image.open( io.BytesIO( base64.b64decode( image_base64 ) ) ).resize( ( 200 , 200 ) ).convert( 'RGB')
    image = np.asarray( image )
    # Perform scaling, as required by the model
    image = np.expand_dims( image , axis=0 ) / 255.0
    # Perform inference
    predictions = model.predict( image ) * 116.0
    # Return the results as a JSON string
    return jsonify( prob=predictions[ 0 ].tolist() )

if __name__ == "__main__":
    app.run( debug=True )