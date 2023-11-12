<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Concerns\HasUlids;

class Image extends Model
{
    use HasFactory,HasUlids;

    public $table = 'images';

    protected $primaryKey = 'id';
    protected $fillable = [
        'title',
        'path',
        'url',
    ];


}

