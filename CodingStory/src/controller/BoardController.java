package controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import dao.FreeBoardDAO;
import dao.VideoBoardDAO;
import model.FreePostDTO;
import model.VideoPostDTO;

/**
 * Servlet implementation class BoardController
 */
@WebServlet("/board/*")
public class BoardController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] uriTokens = request.getRequestURI().split("/");
		String action = uriTokens[uriTokens.length - 1];
		
		if (action.equals("free")) {
			goFreeBoardPage(request, response);
			
		} else if (action.equals("video")) {
			goVideoBoardPage(request, response);
			
		} else if (action.equals("view")) { // 글 내용 요청
			String board = uriTokens[uriTokens.length - 2];// '게시판구분'/view (어느게시판 관련 요청인지)
			int boardId;
			try {
				boardId = Integer.parseInt(request.getParameter("boardId"));
			} catch (Exception e) {
				request.getSession().setAttribute("message", "잘못된 요청입니다.");
				request.setAttribute("title", "페이지 요청 오류");
				request.getRequestDispatcher("/error404").forward(request, response);
				return;
			}
			String nextPage = "";//다음으로 이동하게될 경로
			if (board.equals("free") && boardId > 0) {
				FreePostDTO freePost = new FreeBoardDAO().getPost(boardId);
				new FreeBoardDAO().increseViews(boardId);
				request.setAttribute("post", freePost);
				request.setAttribute("title", freePost.getBoardTitle());
				nextPage = "/WEB-INF/views/board/free_post_view.jsp";
				
			} else if (board.equals("video") && boardId > 0) {
				VideoPostDTO videoPost = new VideoBoardDAO().getPost(boardId);
				videoPost.setVideoURL(VideoBoardDAO.getVideoId(videoPost.getVideoURL()));
				new VideoBoardDAO().increseViews(boardId);
				request.setAttribute("post", videoPost);
				request.setAttribute("title", videoPost.getBoardTitle());
				nextPage = "/WEB-INF/views/board/video_post_view.jsp";
			} else {
				request.setAttribute("title", "페이지 요청 오류");
				request.getRequestDispatcher("/error404").forward(request, response);
				return;
			}
			request.getRequestDispatcher(nextPage).forward(request, response);
			
		} else if (action.equals("write.user")) {// .user 요청은 LoginCheckFilter를 통해 회원인지 판단(web.xml설정)
			String boardType = uriTokens[uriTokens.length - 2];// '게시판구분'/write.user (어느게시판 관련 요청인지)
			if (boardType.equals("free")) {
				request.setAttribute("title", "자유게시판");
				request.getRequestDispatcher("/WEB-INF/views/board/free_board_write.jsp").forward(request, response);
				
			} else if (boardType.equals("video")) {
				request.setAttribute("title", "영상게시판");
				request.getRequestDispatcher("/WEB-INF/views/board/video_board_write.jsp").forward(request, response);
				
			} else {
				request.setAttribute("title", "페이지 요청 오류");
				request.getRequestDispatcher("/error404").forward(request, response);
			}
			
		} else if (action.equals("delete.user")) {
			String boardType = uriTokens[uriTokens.length - 2];// '게시판구분'/delete (어느게시판 관련 요청인지)
			HttpSession session = request.getSession();
			String userId = (String)session.getAttribute("user");
			if (boardType.equals("free")) {
				int boardId = 0;
				try {
					boardId = Integer.parseInt(request.getParameter("boardId"));
				} catch (Exception e) {
					session.setAttribute("message", "잘못된 요청입니다.");
					response.sendRedirect(request.getContextPath() + "/board/free");
					return;
				}
				
				// 게시글에 이미지파일이 있다면 같이 지울수 있도록 경로구하기
				String saveDirectory = session.getServletContext().getRealPath("images_upload/");
				
				int result = new FreeBoardDAO().delete(boardId, userId, saveDirectory);
				if (result == 1) {
					session.setAttribute("message", "삭제 되었습니다");
				} else if (result == 0) {
					session.setAttribute("message", "이미 삭제되었거나 없는 게시물입니다.");
				} else {
					session.setAttribute("message", "DB오류 또는 삭제권한이 없습니다.");
				}
				response.sendRedirect(request.getContextPath() + "/board/free");
				
			} else if (boardType.equals("video")) {
				int boardId = 0;
				try {
					boardId = Integer.parseInt(request.getParameter("boardId"));
				} catch (Exception e) {
					session.setAttribute("message", "잘못된 요청입니다.");
					response.sendRedirect(request.getContextPath() + "/board/video");
					return;
				}
				int result = new VideoBoardDAO().delete(boardId, userId);
				if (result == 1) {
					session.setAttribute("message", "삭제 되었습니다");
				} else if (result == 0) {
					session.setAttribute("message", "이미 삭제되었거나 없는 게시물입니다.");
				} else {
					session.setAttribute("message", "DB오류 또는 삭제권한이 없습니다.");
				}
				response.sendRedirect(request.getContextPath() + "/board/video");
				
			} else {
				request.setAttribute("title", "페이지 요청 오류");
				request.getRequestDispatcher("/error404").forward(request, response);
			}
		} else if (action.equals("update")) {
			String boardType = uriTokens[uriTokens.length - 2];// '게시판구분'/update (어느게시판 관련 요청인지)
			HttpSession session = request.getSession();
			if (boardType.equals("free")) {
				int boardId = 0;
				try {
					boardId = Integer.parseInt(request.getParameter("boardId"));
				} catch (Exception e) {
					session.setAttribute("message", "잘못된 요청입니다.");
					response.sendRedirect(request.getContextPath() + "/board/free");
					return;
				}
				
				FreePostDTO post = new FreeBoardDAO().getPost(boardId);
				if (post != null) {
					request.setAttribute("post", post);
					request.setAttribute("title", "수정 페이지");
					request.getRequestDispatcher("/WEB-INF/views/board/free_board_write.jsp").forward(request, response);
					return;
					
				} else {
					session.setAttribute("message", "DB오류로 접근에 실패했습니다.");
					response.sendRedirect(request.getContextPath() + "/board/free");
					return;
				}
				
			} else if (boardType.equals("video")) {
				int boardId = 0;
				try {
					boardId = Integer.parseInt(request.getParameter("boardId"));
				} catch (Exception e) {
					session.setAttribute("message", "잘못된 요청입니다.");
					response.sendRedirect(request.getContextPath() + "/board/video");
					return;
				}
				
				VideoPostDTO post = new VideoBoardDAO().getPost(boardId);
				if (post != null) {
					request.setAttribute("post", post);
					request.setAttribute("title", "수정 페이지");
					request.getRequestDispatcher("/WEB-INF/views/board/video_board_write.jsp").forward(request, response);
					return;
					
				} else {
					session.setAttribute("message", "DB오류로 접근에 실패했습니다.");
					response.sendRedirect(request.getContextPath() + "/board/video");
					return;
				}
				
			} else {
				request.setAttribute("title", "페이지 요청 오류");
				request.getRequestDispatcher("/error404").forward(request, response);
			}
		} else if (action.equals("search")) {
			String searchArea = request.getParameter("searchArea");
			String search = request.getParameter("search");
			if (searchArea == null || search == null) {
				request.getSession().setAttribute("message", "잘못된 접근입니다.");
				response.sendRedirect(request.getContextPath().equals("") ? "/" : request.getContextPath());
			}
			
			if (searchArea.equals("all")) {
				//영상게시판
				List<VideoPostDTO> videoPostList = new VideoBoardDAO().getBoardList(search);
				if (videoPostList != null) {
					request.setAttribute("videoPostList", videoPostList);
				}
				
				//자유게시판
				List<FreePostDTO> freePostList = new FreeBoardDAO().getBoardList(search);
				if (freePostList != null) {
					request.setAttribute("freePostList", freePostList);
				}
				
				request.setAttribute("title", "종합 검색 결과");
				request.getRequestDispatcher("/WEB-INF/views/board/search_result_page.jsp").forward(request, response);
				
			} else if (searchArea.equals("free")) {
				List<FreePostDTO> freePostList = new FreeBoardDAO().getBoardList(search);
				if (freePostList != null) {
					request.setAttribute("freePostList", freePostList);
				}
				request.setAttribute("title", "자유게시판 검색");
				request.getRequestDispatcher("/WEB-INF/views/board/free_board.jsp").forward(request, response);
				
			} else if (searchArea.equals("video")) {
				List<VideoPostDTO> videoPostList = new VideoBoardDAO().getBoardList(search);
				if (videoPostList != null) {
					request.setAttribute("videoPostList", videoPostList);
				}
				request.setAttribute("title", "영상게시판 검색");
				request.getRequestDispatcher("/WEB-INF/views/board/video_board.jsp").forward(request, response);
				
			} else {
				request.setAttribute("title", "페이지 요청 오류");
				request.getRequestDispatcher("/error404").forward(request, response);
			}
		} else {
			request.setAttribute("title", "페이지 요청 오류");
			request.getRequestDispatcher("/error404").forward(request, response);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] uriTokens = request.getRequestURI().split("/");
		String action = uriTokens[uriTokens.length - 1];
		HttpSession session = request.getSession();
		
		if (action.equals("free")) { // insert, update 요청 처리
			String saveDirectory = session.getServletContext().getRealPath("images_upload/");
			int maxPostSize = 1024 * 1024 * 10;
			String encoding = "UTF-8";
			
			MultipartRequest multipartRequest = null;
			try {
				multipartRequest = new MultipartRequest(request, saveDirectory, maxPostSize, encoding, new DefaultFileRenamePolicy());
			} catch (Exception e) {
				session.setAttribute("message", "업로드 실패. 파일 크기는 10MB를 넘을 수 없습니다.");
				response.sendRedirect(request.getContextPath() + "/board/free");
				return;
			}
			String boardId = multipartRequest.getParameter("boardId");
			
			if (boardId == null) { //insert
				String title = multipartRequest.getParameter("boardTitle");
				String content = multipartRequest.getParameter("boardContent");
				String imgFileName = multipartRequest.getOriginalFileName("imgFileName");
				String imgFileRealName = multipartRequest.getFilesystemName("imgFileName");
				String user = (String)request.getSession().getAttribute("user");
				
				//파일이 넘어 왔는데 그것이 jpg,png,gif 파일이 아니라면 등록거부
				if (imgFileName != null) {
					String ext = imgFileName.substring(imgFileName.lastIndexOf(".") + 1).toLowerCase();
					if (!ext.equals("jpg") && !ext.equals("png") && !ext.equals("gif")) {
						File file = new File(saveDirectory + imgFileRealName);
						file.delete();
						session.setAttribute("message", "업로드실패. 파일추가는 사진파일(jpg,png,gif)만 가능합니다");
						response.sendRedirect(request.getContextPath() + "/board/free");
						return;
					}
				}
				
				FreePostDTO freePost = new FreePostDTO();
				freePost.setBoardTitle(title);
				freePost.setBoardContent(content);
				freePost.setUserId(user);
				freePost.setImgFileName(imgFileName);
				freePost.setImgFileRealName(imgFileRealName);
				
				int result = new FreeBoardDAO().insert(freePost);
				if (result > 0) {
					session.setAttribute("message", "글 작성이 완료되었습니다.");
				} else {
					session.setAttribute("message", "DB오류로 게시물 작성에 실패하였습니다.");
				}
				
			} else { // update
				int parseBoardId = Integer.parseInt(boardId); // post요청이니까 예외처리x?
				FreePostDTO post = new FreeBoardDAO().getPost(parseBoardId);
				
				// 얻어온 게시글의 작성자와 수정요청한 사람이 일치하는지 확인
				String insertId = post.getUserId();
				String updateId = (String)session.getAttribute("user");
				if (insertId.equals(updateId)) {
					String title = multipartRequest.getParameter("boardTitle");
					String content = multipartRequest.getParameter("boardContent");
					String imgFileName = multipartRequest.getOriginalFileName("imgFileName");
					String imgFileRealName = multipartRequest.getFilesystemName("imgFileName");
					String fileNameViewer = multipartRequest.getParameter("fileNameViewer");
					
					if (fileNameViewer.equals("")) {// fileNameViewer 값이 없다면 결국 이미지는 삭제.
						if (imgFileName != null) {
							File file = new File(saveDirectory + imgFileRealName);// 넘어온파일 지우고
							file.delete();
						}
						String beforeFile = post.getImgFileRealName();
						if (beforeFile != null) {
							File file = new File(saveDirectory + beforeFile);// 기존파일도 지우고
							file.delete();
						}
						imgFileName = null;
						imgFileRealName = null;
					} else {
						if (imgFileName == null) { // fileNameViewer 값이 있고 넘어온 파일은 없다면 기존파일 유지
							imgFileName = post.getImgFileName();
							imgFileRealName = post.getImgFileRealName();
						} else {
							//파일이 넘어 왔는데 그것이 jpg,png,gif 파일이 아니라면 수정거부
							String ext = imgFileName.substring(imgFileName.lastIndexOf(".") + 1).toLowerCase();
							if (!ext.equals("jpg") && !ext.equals("png") && !ext.equals("gif")) {
								File file = new File(saveDirectory + imgFileRealName);
								file.delete();
								session.setAttribute("message", "수정실패. 파일은 사진파일(jpg,png,gif)만 가능합니다");
								response.sendRedirect(request.getContextPath() + "/board/free");
								return;
							}
							String beforeFile = post.getImgFileRealName();
							if (beforeFile != null) {
								File file = new File(saveDirectory + beforeFile);// 기존파일 지우기
								file.delete();
							}
						}
					}

					int result = new FreeBoardDAO().update(parseBoardId, title, content, imgFileName, imgFileRealName);
					if (result == 1) {
						session.setAttribute("message", "수정 되었습니다.");
					} else {
						session.setAttribute("message", "DB오류로 수정에 실패하였습니다.");
					}
				} else {
					session.setAttribute("message", "수정 권한이 없습니다.");
				}
			}
			response.sendRedirect(request.getContextPath()+"/board/free");
		} else if (action.equals("video")) { // insert, update 요청 처리
			String boardId = request.getParameter("boardId");
			
			if (boardId == null) { //insert
				String title = request.getParameter("boardTitle");
				String content = request.getParameter("boardContent");
				String videoURL = request.getParameter("videoURL");
				String user = (String)request.getSession().getAttribute("user");
				
				VideoPostDTO videoPost = new VideoPostDTO();
				videoPost.setBoardTitle(title);
				videoPost.setBoardContent(content);
				videoPost.setUserId(user);
				videoPost.setVideoURL(videoURL);
				
				int result = new VideoBoardDAO().insert(videoPost);
				if (result > 0) {
					session.setAttribute("message", "글 작성이 완료되었습니다.");
				} else {
					session.setAttribute("message", "DB오류로 게시물 작성에 실패하였습니다.");
				}
				
			} else { // update
				int parseBoardId = Integer.parseInt(boardId); // post요청이니까 예외처리x?
				VideoPostDTO post = new VideoBoardDAO().getPost(parseBoardId);
				
				// 얻어온 게시글의 작성자와 수정요청한 사람이 일치하는지 확인
				String insertId = post.getUserId();
				String updateId = (String)session.getAttribute("user");
				if (insertId.equals(updateId)) {
					String title = request.getParameter("boardTitle");
					String content = request.getParameter("boardContent");
					String videoURL = request.getParameter("videoURL");
					
					int result = new VideoBoardDAO().update(parseBoardId, title, content, videoURL);
					if (result == 1) {
						session.setAttribute("message", "수정 되었습니다.");
					} else {
						session.setAttribute("message", "DB오류로 수정에 실패하였습니다.");
					}
				} else {
					session.setAttribute("message", "수정 권한이 없습니다.");
				}
			}
			response.sendRedirect(request.getContextPath()+"/board/video");
		} else {
			request.setAttribute("title", "페이지 요청 오류");
			request.getRequestDispatcher("/error404").forward(request, response);
		}
	}

	private void goFreeBoardPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String page = request.getParameter("page");
		if (page == null) page = "1";
		int pageInt = 1;
		try {
			pageInt = Integer.parseInt(page);
		} catch (Exception e) {
			request.getSession().setAttribute("message", "잘못된 페이지 번호입니다.");
			request.setAttribute("title", "페이지 요청 오류");
			request.getRequestDispatcher("/error404").forward(request, response);
			return;
		}
		List<FreePostDTO> freePostList = new FreeBoardDAO().getBoardList(pageInt);
		int pageCount = new FreeBoardDAO().getPageCount(pageInt);
		
		if (freePostList != null && pageCount != -1) {
			//페이징에 필요한 정보 얻기 
			int maxPageCount = FreeBoardDAO.MAX_PAGE_COUNT;
			
			int pageAreaNum = (pageInt - 1) / maxPageCount * maxPageCount;
			int nextPageAreaNum;
			if (pageCount > maxPageCount) {
				nextPageAreaNum = pageAreaNum + pageCount;
				pageCount--;
			} else {
				nextPageAreaNum = -1;
			}
			request.setAttribute("freePostList", freePostList);
			request.setAttribute("pageCount", pageCount);
			request.setAttribute("pageAreaNum", pageAreaNum);
			request.setAttribute("nextPageAreaNum", nextPageAreaNum);
			request.setAttribute("title", "자유게시판");
			request.getRequestDispatcher("/WEB-INF/views/board/free_board.jsp").forward(request, response);
			
		} else {
			request.getSession().setAttribute("message", "데이터 조회에 실패하였습니다.");
			String contextPath = request.getContextPath();
			response.sendRedirect(contextPath.length() == 0 ? "/" : contextPath);
		}
	}
	
	private void goVideoBoardPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String page = request.getParameter("page");
		if (page == null) page = "1";
		int pageInt = 1;
		try {
			pageInt = Integer.parseInt(page);
		} catch (Exception e) {
			request.getSession().setAttribute("message", "잘못된 페이지 번호입니다.");
			request.setAttribute("title", "페이지 요청 오류");
			request.getRequestDispatcher("/error404").forward(request, response);
			return;
		}
		List<VideoPostDTO> videoPostList = new VideoBoardDAO().getBoardList(pageInt);
		int pageCount = new VideoBoardDAO().getPageCount(pageInt);
		
		if (videoPostList != null && pageCount != -1) {
			//페이징에 필요한 정보 얻기 
			int maxPageCount = VideoBoardDAO.MAX_PAGE_COUNT;
			
			int pageAreaNum = (pageInt - 1) / maxPageCount * maxPageCount;
			int nextPageAreaNum;
			if (pageCount > maxPageCount) {
				nextPageAreaNum = pageAreaNum + pageCount;
				pageCount--;
			} else {
				nextPageAreaNum = -1;
			}
			request.setAttribute("videoPostList", videoPostList);
			request.setAttribute("pageCount", pageCount);
			request.setAttribute("pageAreaNum", pageAreaNum);
			request.setAttribute("nextPageAreaNum", nextPageAreaNum);
			request.setAttribute("title", "영상게시판");
			request.getRequestDispatcher("/WEB-INF/views/board/video_board.jsp").forward(request, response);
			
		} else {
			request.getSession().setAttribute("message", "데이터 조회에 실패하였습니다.");
			String contextPath = request.getContextPath();
			response.sendRedirect(contextPath.length() == 0 ? "/" : contextPath);
		}
	}

}
