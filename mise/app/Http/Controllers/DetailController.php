<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class DetailController extends Controller
{
    //
    public function index(Request $request)
    {
        return view('detail');

    }

    //
    public function buy(Request $request)
    {
        return view('buy');
    }


    //
    public function complete(Request $request)
    {
        return view('complete');
    }

}
