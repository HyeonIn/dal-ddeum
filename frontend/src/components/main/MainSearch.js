import axios from "axios";
import React, { useState } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router";
import { setCurrentPage, setMovieList, setSearchKeyword, setTotalPage } from "../../feature/reducer/MovieReducer";

function MainSearch() {
  const movePage = useNavigate();
  const [keyword, setKeyword] = useState("");
  const [btnView, setBtnView] = useState(false);

  function goMovieList() {
    movePage("/search/");
  }

  const keywordHandler = (event) => {
    setKeyword(event.target.value);
  };

  const handleOnKeyPress = (e) => {
    if (e.key === "Enter") {
      searchMovies(); // Enter 입력이 되면 클릭 이벤트 실행
    }
  };

  function onFocusHandler (){
    setBtnView(true);
  } 
  function onBlurHandler(){
    setBtnView(false);
  }


  const dispatch = useDispatch();

  const tmdbToken = process.env.REACT_APP_TMDB_TOKEN;

  const searchMovies = async () => {
    axios
      .get(
        "https://api.themoviedb.org/3/search/movie?api_key=" +
          tmdbToken +
          "&language=ko-KR&page=1&include_adult=false&query=" +
          keyword +
          "&page=1"
      )
      .then((response) => {
        dispatch(setMovieList(response.data.results));
        dispatch(setTotalPage(response.data.total_pages));
        dispatch(setSearchKeyword(keyword));
        dispatch(setCurrentPage(1));
      })
      .then(() => {
        if (keyword === "") {
          console.log("검색어 입력 필요");
        } else {
          goMovieList();
        }
      });
  };

  return (
    <div className="mainSearch">
      <div className="flex justify-center my-16">
        <p className="mr-2 text-dal-orange text-7xl">달:-뜨다</p>
        <div className="mt-6 ml-2">
          <p className="text-white">동사 [마음이]</p>
          <p className="text-white"> 가라앉지 않고 들썽거리다.</p>
        </div>
      </div>
      <div className="my-6 text-center">
        <p className="text-2xl text-white">
          영화보고 달뜬 마음,{" "}
          <span className="text-3xl text-dal-orange">달뜸</span>으로 가져오세요.
        </p>
      </div>
      <div className="flex h-10 mx-20 mt-10">
        <input
          type="text"
          onChange={keywordHandler}
          onKeyPress={handleOnKeyPress}
          onFocus={onFocusHandler}
          onBlur={onBlurHandler}
          id="party-search"
          className="block w-full h-10 pl-4 mx-2 text-lg text-white transition-transform duration-500 bg-transparent border-b-2 focus:w-5/6 placeholder:text-gray-300 focus:outline-none focus:border-dal-orange"
          placeholder="어떤 영화를 보고 오셨나요?"
        />
        {btnView && (
        <button
          onClick={searchMovies}
          className="w-1/6 text-center text-white transition-colors duration-200 hover:bg-dal-orange hover:bg-opacity-70 hover:text-black"
        >검색</button>)}
      </div>
    </div>
  );
}

export default MainSearch;
