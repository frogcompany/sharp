<?php

namespace App\Http\Controllers\Api;

use App\Http\Controllers\Controller;
use App\Models\Image;
use Illuminate\Http\Request;

class UploadController extends Controller
{

    public function store(Request $request)
    {
        if (!$request->hasFile('fileName')) {
            return response()->json(['upload_file_not_found'], 400);
        }

        $allowedFileExtension = ['pdf', 'jpg', 'png'];
        $files = $request->file('fileName');
        $errors = [];
        $array = [];

        foreach ($files as $file) {

            $extension = $file->getClientOriginalExtension();

            $check = in_array($extension, $allowedFileExtension);

            if ($check) {
                foreach ($request->fileName as $mediaFiles) {

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
//            $array
        }

        // エラー確認
        if(count($errors)>0){
            return response()->json(['error'], 422);
        }

        $array = array(
            0 => array(
                "id"   => 1,
                "genre" => "服",
                "title" => "誰でもイケメンになる服",
                "bodys" => "ドラマできてたやつ",
                "url" => "https://miseai.site/detail/1/",
                "img" => "https://miseai.site/img/fukudammy.jpg",
                "price" => "10,000",
                "tax" => "1,000",
                "pricetax" => "11,000",
                "etc" => "SIZE:XL COLOR:BLUE"
            ),

            1 => array(
                "id"   => 2,
                "genre" => "食品",
                "title" => "サッポロ一番味噌ラーメン",
                "bodys" => "アニメで小池さんが食べてた食品",
                "url" => "https://miseai.site/detail/2/",
                "img" => "https://miseai.site/img/koikedammy.jpg",
                "price" => "200",
                "tax" => "16",
                "pricetax" => "216",
                "etc" => "サンヨー食品、一袋 (軽減税率対象)"
            )

        );

        return response()->json($array, 200);

    }

}
