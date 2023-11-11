<?php
namespace App\Http\Controllers\Api;
 
use App\Http\Controllers\Controller;
use App\Models\Image;
use Validator;
use Illuminate\Http\Request;
 
class UploadController extends Controller
{
 
public function store(Request $request)
{
    if(!$request->hasFile('fileName')) {
        return response()->json(['upload_file_not_found'], 400);
    }
 
    $allowedfileExtension=['pdf','jpg','png'];
    $files = $request->file('fileName'); 
    $errors = [];
 
    foreach ($files as $file) {      
 
        $extension = $file->getClientOriginalExtension();
 
        $check = in_array($extension,$allowedfileExtension);
 
        if($check) {
            foreach($request->fileName as $mediaFiles) {
 
                $path = $mediaFiles->store('public/images');
                $name = $mediaFiles->getClientOriginalName();
      
                //store image file into directory and db
                $save = new Image();
                $save->title = $name;
                $save->path = $path;
                $save->save();
            }
        } else {
            return response()->json(['invalid_file_format'], 422);
        }
 
        return response()->json(['file_uploaded'], 200);
 
    }
 

}

}
