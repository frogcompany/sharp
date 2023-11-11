<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class ChatGptController extends Controller
{
    /**
     * index
     *
     * @param  Request  $request
     */
    public function index(Request $request)
    {
     return view('chat');

    }

    /**
     * chat
     *
     * @param  Request  $request
     */
    public function chat(Request $request)
    {
    }


//
}
